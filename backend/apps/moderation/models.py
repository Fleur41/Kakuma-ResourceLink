from django.conf import settings
from django.db import models
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel


class TargetType(models.TextChoices):
    STORY = "story", _("Story")
    COMMENT = "comment", _("Comment")
    PROJECT = "project", _("Community project")


class ReportStatus(models.TextChoices):
    OPEN = "open", _("Open")
    RESOLVED = "resolved", _("Resolved")
    DISMISSED = "dismissed", _("Dismissed")


class ReportReason(models.TextChoices):
    SPAM = "spam", _("Spam")
    HARASSMENT = "harassment", _("Harassment or hate")
    PRIVACY = "privacy", _("Shares private information")
    MISINFORMATION = "misinformation", _("False or misleading")
    SCAM = "scam", _("Scam or fraud")
    OTHER = "other", _("Other")


class Report(UUIDModel):
    reporter = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="reports")
    target_type = models.CharField(max_length=10, choices=TargetType.choices)
    target_id = models.UUIDField()
    reason = models.CharField(max_length=20, choices=ReportReason.choices)
    details = models.TextField(max_length=1000, blank=True)
    status = models.CharField(max_length=10, choices=ReportStatus.choices, default=ReportStatus.OPEN)

    class Meta:
        ordering = ["-created_at"]
        indexes = [models.Index(fields=["status", "target_type"])]
        constraints = [
            models.UniqueConstraint(
                fields=["reporter", "target_type", "target_id"],
                condition=models.Q(status="open"),
                name="unique_open_report_per_reporter",
            )
        ]


class DecisionAction(models.TextChoices):
    APPROVE = "approve", _("Approve")
    REJECT = "reject", _("Reject")
    HIDE = "hide", _("Hide")
    RESOLVE_REPORT = "resolve_report", _("Resolve report")
    DISMISS_REPORT = "dismiss_report", _("Dismiss report")


class ModerationDecision(UUIDModel):
    """Audit trail of every moderator action."""

    moderator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, related_name="+")
    target_type = models.CharField(max_length=10, choices=TargetType.choices)
    target_id = models.UUIDField()
    action = models.CharField(max_length=20, choices=DecisionAction.choices)
    note = models.TextField(blank=True)

    class Meta:
        ordering = ["-created_at"]
        indexes = [models.Index(fields=["target_type", "target_id"])]
