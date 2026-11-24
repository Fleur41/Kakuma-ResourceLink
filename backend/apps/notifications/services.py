"""Single entry point for creating notifications so every module produces a consistent payload."""
from django.utils import timezone, translation

from .models import Notification


def notify(
    recipient, type: str, title, body="", data: dict | None = None, params: dict | None = None
) -> Notification | None:
    """Create an in-app notification.

    Pass *lazy* translatable strings plus `params`; they are rendered inside the recipient's
    preferred language so each user receives notifications in their own language.
    """
    if recipient is None or not recipient.is_active:
        return None
    with translation.override(getattr(recipient, "preferred_language", None) or "en"):
        title_text = str(title) % params if params else str(title)
        body_text = str(body) % params if params and body else str(body)
        return Notification.objects.create(
            recipient=recipient, type=type, title=title_text, body=body_text, data=data or {}
        )


def mark_read(notification: Notification) -> Notification:
    if not notification.is_read:
        notification.is_read = True
        notification.read_at = timezone.now()
        notification.save(update_fields=["is_read", "read_at", "updated_at"])
    return notification


def mark_all_read(user) -> int:
    return Notification.objects.filter(recipient=user, is_read=False).update(is_read=True, read_at=timezone.now())
