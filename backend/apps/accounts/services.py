"""Account business logic kept out of views so it can be reused and tested directly."""
import logging
import secrets
from datetime import timedelta

from django.conf import settings
from django.core.mail import send_mail
from django.db import transaction
from django.db.models import F
from django.utils import timezone, translation
from django.utils.crypto import constant_time_compare, salted_hmac
from django.utils.translation import gettext as _
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken, OutstandingToken
from rest_framework_simplejwt.tokens import RefreshToken

from .models import PasswordResetCode, User

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


def _code_hash(user: User, code: str) -> str:
    return salted_hmac("accounts.password_reset_code", f"{user.pk}:{code}", algorithm="sha256").hexdigest()


def send_password_reset(email: str) -> None:
    """Email a 6-digit reset code if the account exists. Silent otherwise to avoid account enumeration."""
    user = User.objects.filter(email__iexact=email, is_active=True).first()
    if not user:
        return
    latest = user.password_reset_codes.filter(used_at__isnull=True).first()
    if latest and (timezone.now() - latest.created_at).total_seconds() < settings.PASSWORD_RESET_RESEND_SECONDS:
        return  # a code was just sent; don't flood the inbox
    code = f"{secrets.randbelow(10**6):06d}"
    minutes = settings.PASSWORD_RESET_CODE_MINUTES
    with transaction.atomic():
        user.password_reset_codes.all().delete()  # only the newest code works
        PasswordResetCode.objects.create(
            user=user, code_hash=_code_hash(user, code), expires_at=timezone.now() + timedelta(minutes=minutes)
        )
    with translation.override(user.preferred_language):
        subject = _("Your Kakuma ResourceLink password reset code")
        message = _(
            "Hello %(name)s,\n\n"
            "Your password reset code is: %(code)s\n\n"
            "Enter this code in the Kakuma ResourceLink app or website within %(minutes)d minutes to choose a new password.\n\n"
            "If you didn't ask to reset your password, you can ignore this email. Your password won't change."
        ) % {"name": user.full_name.split(" ")[0] or user.email, "code": code, "minutes": minutes}
    try:
        send_mail(subject, message, settings.DEFAULT_FROM_EMAIL, [user.email], fail_silently=False)
    except Exception:
        # Keep the same response either way, so a mail failure doesn't reveal that the account exists.
        logger.exception("Could not send the password reset email")


def check_reset_code(email: str, code: str) -> PasswordResetCode | None:
    """The live reset code matching `code`, or None. Each wrong guess counts towards the attempt limit."""
    user = User.objects.filter(email__iexact=email, is_active=True).first()
    if not user:
        return None
    record = (
        user.password_reset_codes.filter(used_at__isnull=True, expires_at__gt=timezone.now())
        .select_related("user")
        .first()
    )
    if not record or record.attempts >= settings.PASSWORD_RESET_CODE_ATTEMPTS:
        return None
    if not constant_time_compare(record.code_hash, _code_hash(user, code)):
        PasswordResetCode.objects.filter(pk=record.pk).update(attempts=F("attempts") + 1)
        return None
    return record


@transaction.atomic
def reset_password_with_code(record: PasswordResetCode, new_password: str) -> None:
    record.used_at = timezone.now()
    record.save(update_fields=["used_at"])
    set_new_password(record.user, new_password)


@transaction.atomic
def set_new_password(user: User, new_password: str) -> None:
    user.set_password(new_password)
    user.save(update_fields=["password"])
    revoke_all_refresh_tokens(user)
