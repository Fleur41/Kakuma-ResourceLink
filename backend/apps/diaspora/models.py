from django.conf import settings
from django.db import models
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel


class Visibility(models.TextChoices):
    PUBLIC = "public", _("Everyone")
    MEMBERS = "members", _("Signed-in members only")
    PRIVATE = "private", _("Only me")


class AlumniProfile(UUIDModel):
    """Voluntary diaspora registration. Nothing is shown beyond what the alumnus consents to."""

    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="alumni_profile")
    country = models.CharField(max_length=80)
    city = models.CharField(max_length=80, blank=True)
    profession = models.CharField(max_length=120)
    organization = models.CharField(max_length=150, blank=True)
    skills = models.JSONField(default=list, blank=True)
    bio = models.TextField(max_length=2000, blank=True)
    is_mentor = models.BooleanField(default=False, help_text="Willing to mentor")
    mentorship_areas = models.JSONField(default=list, blank=True)
    willing_to_contribute = models.BooleanField(default=False)
    contribution_interests = models.JSONField(default=list, blank=True)
    visibility = models.CharField(max_length=10, choices=Visibility.choices, default=Visibility.MEMBERS)
    show_contact_email = models.BooleanField(default=False)
    show_city = models.BooleanField(default=False)
    consent_given_at = models.DateTimeField()

    class Meta:
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["visibility", "is_mentor"]),
            models.Index(fields=["country"]),
        ]

    def __str__(self) -> str:
        return f"Alumni<{self.user_id}>"
