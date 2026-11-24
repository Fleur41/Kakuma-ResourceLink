import uuid

from django.conf import settings
from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.db import models
from django.db.models.functions import Lower
from django.utils import timezone
from django.utils.translation import gettext_lazy as _

from apps.common.models import TimeStampedModel
from apps.common.uploads import randomized_upload_to
from apps.common.validators import IMAGE_VALIDATORS


class Role(models.TextChoices):
    MEMBER = "member", _("Community member")
    ALUMNI = "alumni", _("Alumni")
    MENTOR = "mentor", _("Mentor")
    MODERATOR = "moderator", _("Moderator")
    ADMIN = "admin", _("Administrator")


SELF_ASSIGNABLE_ROLES = {Role.MEMBER, Role.ALUMNI, Role.MENTOR}


class Language(models.TextChoices):
    EN = "en", "English"
    SW = "sw", "Kiswahili"
    ES = "es", "Español"
    IT = "it", "Italiano"
    DE = "de", "Deutsch"


class UserManager(BaseUserManager):
    use_in_migrations = True

    def _create_user(self, email, password, **extra):
        if not email:
            raise ValueError("Email is required")
        email = self.normalize_email(email).lower()
        user = self.model(email=email, **extra)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_user(self, email, password=None, **extra):
        extra.setdefault("is_staff", False)
        extra.setdefault("is_superuser", False)
        return self._create_user(email, password, **extra)

    def create_superuser(self, email, password=None, **extra):
        extra.update(is_staff=True, is_superuser=True, role=Role.ADMIN)
        return self._create_user(email, password, **extra)


class User(AbstractBaseUser, PermissionsMixin):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    email = models.EmailField(_("email address"), unique=True)
    full_name = models.CharField(max_length=150)
    role = models.CharField(max_length=20, choices=Role.choices, default=Role.MEMBER)
    preferred_language = models.CharField(max_length=5, choices=Language.choices, default=Language.EN)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    date_joined = models.DateTimeField(default=timezone.now)

    objects = UserManager()

    USERNAME_FIELD = "email"
    EMAIL_FIELD = "email"
    REQUIRED_FIELDS = ["full_name"]

    class Meta:
        ordering = ["-date_joined"]
        constraints = [models.UniqueConstraint(Lower("email"), name="accounts_user_email_ci_unique")]

    def __str__(self) -> str:
        return self.email

    @property
    def is_moderator(self) -> bool:
        return self.is_staff or self.role in {Role.MODERATOR, Role.ADMIN}


class Profile(TimeStampedModel):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="profile")
    bio = models.TextField(blank=True, max_length=1000)
    location = models.CharField(max_length=120, blank=True)
    avatar = models.ImageField(upload_to=randomized_upload_to("avatars"), blank=True, validators=IMAGE_VALIDATORS)
    # Private: never exposed through public serializers.
    phone = models.CharField(max_length=30, blank=True)

    def __str__(self) -> str:
        return f"Profile<{self.user_id}>"
