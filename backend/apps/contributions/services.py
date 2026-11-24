from decimal import Decimal

from django.conf import settings
from django.db.models import Count, Sum
from django.utils.translation import gettext_lazy as _
from rest_framework.exceptions import ValidationError

from apps.common.exceptions import ConflictError
from apps.notifications.models import NotificationType
from apps.notifications.services import notify

from .models import CommunityProject, Contribution, ContributionStatus, ContributionType, ProjectStatus


def initial_project_status(user) -> str:
    if not settings.PROJECTS_REQUIRE_REVIEW or user.is_moderator:
        return ProjectStatus.ACTIVE
    return ProjectStatus.PENDING_REVIEW


def add_contribution(project: CommunityProject, contributor, *, type: str, description: str, amount=None) -> Contribution:
    if project.status != ProjectStatus.ACTIVE:
        raise ConflictError(_("This project is not accepting contributions right now."))
    if project.accepted_contribution_types and type not in project.accepted_contribution_types:
        raise ValidationError({"type": [_("This project does not accept this type of contribution.")]})
    if type == ContributionType.FINANCIAL_PLEDGE:
        if amount is None or amount <= 0:
            raise ValidationError({"amount": [_("Enter the amount you are pledging.")]})
        currency = project.currency
    else:
        amount, currency = None, ""
    contribution = Contribution.objects.create(
        project=project, contributor=contributor, type=type, description=description,
        amount=amount, currency=currency, status=ContributionStatus.PLEDGED,
    )
    if project.owner_id != contributor.id:
        notify(
            project.owner, NotificationType.PROJECT_CONTRIBUTION, _("New contribution pledged"),
            _("%(name)s pledged support to “%(title)s”."),
            {"project_id": str(project.id), "contribution_id": str(contribution.id)},
            params={"name": contributor.full_name, "title": project.title},
        )
    return contribution


def contribution_summary(project: CommunityProject) -> dict:
    active = project.contributions.exclude(status=ContributionStatus.CANCELLED)
    by_type = {row["type"]: row["n"] for row in active.values("type").annotate(n=Count("id"))}
    pledged = active.filter(type=ContributionType.FINANCIAL_PLEDGE).aggregate(total=Sum("amount"))["total"]
    return {
        "total_contributions": sum(by_type.values()),
        "pledged_amount": str(pledged or Decimal("0.00")),
        "currency": project.currency,
        "by_type": by_type,
        "note": "Pledged amounts are commitments, not completed payments.",
    }
