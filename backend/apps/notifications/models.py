from django.conf import settings
from django.db import models
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel


class NotificationType(models.TextChoices):
    MENTORSHIP_REQUEST = "mentorship_request", _("New mentorship request")
    MENTORSHIP_UPDATE = "mentorship_update", _("Mentorship request update")
    STORY_PUBLISHED = "story_published", _("Story published")
    STORY_REJECTED = "story_rejected", _("Story needs changes")
    STORY_COMMENT = "story_comment", _("New comment")
    PROJECT_CONTRIBUTION = "project_contribution", _("New contribution")
    PROJECT_STATUS = "project_status", _("Project status update")
    SYSTEM = "system", _("System")


class Notification(UUIDModel):
    recipient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="notifications")
    type = models.CharField(max_length=30, choices=NotificationType.choices)
    title = models.CharField(max_length=200)
    body = models.TextField(blank=True)
    # Deep-link payload for clients, e.g. {"story_id": "..."}.
    data = models.JSONField(default=dict, blank=True)
    is_read = models.BooleanField(default=False)
    read_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]
        indexes = [models.Index(fields=["recipient", "is_read", "-created_at"])]
