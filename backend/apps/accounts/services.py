"""Account business logic kept out of views so it can be reused and tested directly."""
import logging

from django.conf import settings
from django.contrib.auth.tokens import default_token_generator
from django.core.mail import send_mail
from django.db import transaction
from django.utils.encoding import force_bytes, force_str
from django.utils.http import urlsafe_base64_decode, urlsafe_base64_encode
from django.utils.translation import gettext as _
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken, OutstandingToken
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User

logger = logging.getLogger(__name__)


def issue_tokens(user: User) -> dict:
    refresh = RefreshToken.for_user(user)
    return {"access": str(refresh.access_token), "refresh": str(refresh)}


@transaction.atomic
def register_user(*, email: str, password: str, full_name: str, role: str, preferred_language: str) -> User:
    return User.objects.create_user(
        email=email, password=password, full_name=full_name, role=role, preferred_language=preferred_language
    )


def revoke_all_refresh_tokens(user: User) -> None:
    """Invalidate every refresh token of a user (password change/reset)."""
    for token in OutstandingToken.objects.filter(user=user):
        BlacklistedToken.objects.get_or_create(token=token)


def send_password_reset(email: str) -> None:
    """Send a reset link if the account exists. Silent otherwise to avoid account enumeration."""
    user = User.objects.filter(email__iexact=email, is_active=True).first()
    if not user:
        return
    uid = urlsafe_base64_encode(force_bytes(user.pk))
    token = default_token_generator.make_token(user)
    link = settings.PASSWORD_RESET_URL.format(uid=uid, token=token)
    send_mail(
        subject=_("Reset your Kakuma ResourceLink password"),
        message=_("Use this link to choose a new password: %(link)s\nIf you did not request this, ignore this email.")
        % {"link": link},
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[user.email],
        fail_silently=False,
    )


def user_from_reset_uid(uid: str) -> User | None:
    try:
        pk = force_str(urlsafe_base64_decode(uid))
        return User.objects.get(pk=pk, is_active=True)
    except (TypeError, ValueError, OverflowError, User.DoesNotExist):
        return None


@transaction.atomic
def set_new_password(user: User, new_password: str) -> None:
    user.set_password(new_password)
    user.save(update_fields=["password"])
    revoke_all_refresh_tokens(user)
