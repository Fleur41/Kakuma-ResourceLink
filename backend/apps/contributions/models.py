from decimal import Decimal

from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import models
from django.db.models import Q
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel
from apps.common.uploads import randomized_upload_to
from apps.common.validators import IMAGE_VALIDATORS


class ProjectStatus(models.TextChoices):
    PENDING_REVIEW = "pending_review", _("In review")
    ACTIVE = "active", _("Active")
    COMPLETED = "completed", _("Completed")
    REJECTED = "rejected", _("Not approved")


class ProjectCategory(models.TextChoices):
    EDUCATION = "education", _("Education")
    HEALTH = "health", _("Health")
    LIVELIHOOD = "livelihood", _("Livelihood")
    TECHNOLOGY = "technology", _("Technology")
    ENVIRONMENT = "environment", _("Environment")
    YOUTH = "youth", _("Youth & sports")
    OTHER = "other", _("Other")


class ContributionType(models.TextChoices):
    MENTORING = "mentoring", _("Mentoring")
    VOLUNTEERING = "volunteering", _("Volunteering")
    EQUIPMENT = "equipment", _("Equipment")
    SKILLS = "skills", _("Professional skills")
    FINANCIAL_PLEDGE = "financial_pledge", _("Financial pledge")


class ContributionStatus(models.TextChoices):
    # A pledge is an intention. The platform does not process payments.
    PLEDGED = "pledged", _("Pledged")
    CONFIRMED = "confirmed", _("Confirmed by project")
    FULFILLED = "fulfilled", _("Fulfilled")
    CANCELLED = "cancelled", _("Cancelled")


class CommunityProject(UUIDModel):
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="community_projects")
    title = models.CharField(max_length=200)
    summary = models.CharField(max_length=300)
    description = models.TextField(max_length=10_000)
    category = models.CharField(max_length=20, choices=ProjectCategory.choices, default=ProjectCategory.OTHER)
    location = models.CharField(max_length=150, default="Kakuma")
    cover_image = models.ImageField(
        upload_to=randomized_upload_to("projects/covers"), blank=True, validators=IMAGE_VALIDATORS
    )
    status = models.CharField(max_length=20, choices=ProjectStatus.choices, default=ProjectStatus.PENDING_REVIEW)
    accepted_contribution_types = models.JSONField(default=list, blank=True)
    funding_goal = models.DecimalField(
        max_digits=12, decimal_places=2, null=True, blank=True, validators=[MinValueValidator(Decimal("1"))]
    )
    currency = models.CharField(max_length=3, default="USD")

    class Meta:
        ordering = ["-created_at"]
        indexes = [models.Index(fields=["status", "category"])]

    def __str__(self) -> str:
        return self.title


class Contribution(UUIDModel):
    project = models.ForeignKey(CommunityProject, on_delete=models.CASCADE, related_name="contributions")
    contributor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="contributions")
    type = models.CharField(max_length=20, choices=ContributionType.choices)
    description = models.TextField(max_length=1000)
    amount = models.DecimalField(max_digits=12, decimal_places=2, null=True, blank=True)
    currency = models.CharField(max_length=3, blank=True)
    status = models.CharField(max_length=12, choices=ContributionStatus.choices, default=ContributionStatus.PLEDGED)

    class Meta:
        ordering = ["-created_at"]
        indexes = [models.Index(fields=["project", "status"])]
        constraints = [
            models.CheckConstraint(
                condition=(Q(type="financial_pledge", amount__isnull=False, amount__gt=0) | (~Q(type="financial_pledge") & Q(amount__isnull=True))),
                name="contribution_amount_matches_type",
            )
        ]
