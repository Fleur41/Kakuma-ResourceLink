from django.conf import settings
from django.db import models
from django.db.models import F, Q
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel


class MentorshipStatus(models.TextChoices):
    PENDING = "pending", _("Pending")
    ACCEPTED = "accepted", _("Accepted")
    DECLINED = "declined", _("Declined")
    CANCELLED = "cancelled", _("Cancelled")
    COMPLETED = "completed", _("Completed")


class HelpArea(models.TextChoices):
    CAREER = "career", _("Career guidance")
    TECHNICAL = "technical", _("Technical skills")
    SCHOLARSHIPS = "scholarships", _("Scholarship applications")
    ENTREPRENEURSHIP = "entrepreneurship", _("Entrepreneurship")
    OTHER = "other", _("Other")


class MentorshipRequest(UUIDModel):
    mentee = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="mentorship_requests_sent")
    mentor = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="mentorship_requests_received"
    )
    goal = models.CharField(max_length=200)
    help_areas = models.JSONField(default=list)
    message = models.TextField(max_length=2000)
    status = models.CharField(max_length=12, choices=MentorshipStatus.choices, default=MentorshipStatus.PENDING)
    response_message = models.TextField(max_length=2000, blank=True)
    responded_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["mentor", "status"]),
            models.Index(fields=["mentee", "status"]),
        ]
        constraints = [
            models.UniqueConstraint(
                fields=["mentee", "mentor"],
                condition=Q(status="pending"),
                name="unique_pending_mentorship_request",
            ),
            models.CheckConstraint(condition=~Q(mentee=F("mentor")), name="mentorship_not_self"),
        ]
