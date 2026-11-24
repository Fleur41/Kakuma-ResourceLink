from django.conf import settings
from django.db import models
from django.utils.translation import gettext_lazy as _

from apps.accounts.models import Language
from apps.common.models import TimeStampedModel, UUIDModel


class OpportunityCategory(models.TextChoices):
    SCHOLARSHIP = "scholarship", _("Scholarship")
    EDUCATION = "education", _("Education")
    VOCATIONAL = "vocational", _("Vocational training")
    JOB = "job", _("Job")
    INTERNSHIP = "internship", _("Internship")
    ENTREPRENEURSHIP = "entrepreneurship", _("Entrepreneurship")


class OpportunityStatus(models.TextChoices):
    DRAFT = "draft", _("Draft")
    PUBLISHED = "published", _("Published")
    ARCHIVED = "archived", _("Archived")


class Opportunity(UUIDModel):
    title = models.CharField(max_length=200)
    category = models.CharField(max_length=20, choices=OpportunityCategory.choices)
    organization = models.CharField(max_length=200)
    summary = models.CharField(max_length=300)
    description = models.TextField()
    requirements = models.JSONField(default=list, blank=True, help_text="List of requirement strings")
    location = models.CharField(max_length=150, blank=True)
    is_remote = models.BooleanField(default=False)
    deadline = models.DateField(null=True, blank=True)
    application_url = models.URLField(blank=True)
    language = models.CharField(max_length=5, choices=Language.choices, default=Language.EN)
    status = models.CharField(max_length=12, choices=OpportunityStatus.choices, default=OpportunityStatus.PUBLISHED)
    # Only staff-verified listings may be marked verified; the AI assistant and UI rely on this flag.
    is_verified = models.BooleanField(default=False)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL, related_name="+"
    )

    class Meta:
        ordering = ["-created_at"]
        verbose_name_plural = "opportunities"
        indexes = [
            models.Index(fields=["status", "category"]),
            models.Index(fields=["deadline"]),
        ]

    def __str__(self) -> str:
        return self.title


class OpportunityBookmark(TimeStampedModel):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="opportunity_bookmarks")
    opportunity = models.ForeignKey(Opportunity, on_delete=models.CASCADE, related_name="bookmarks")

    class Meta:
        constraints = [models.UniqueConstraint(fields=["user", "opportunity"], name="unique_opportunity_bookmark")]
