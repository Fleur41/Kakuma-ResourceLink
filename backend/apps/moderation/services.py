from django.db import transaction
from django.utils.translation import gettext_lazy as _
from rest_framework.exceptions import ValidationError

from apps.common.exceptions import InvalidTransition
from apps.contributions.models import CommunityProject, ProjectStatus
from apps.notifications.models import NotificationType
from apps.notifications.services import notify
from apps.stories import services as story_services
from apps.stories.models import Comment, Story

from .models import DecisionAction, ModerationDecision, Report, ReportStatus, TargetType

TARGET_MODELS = {TargetType.STORY: Story, TargetType.COMMENT: Comment, TargetType.PROJECT: CommunityProject}


def _log(moderator, target_type, target_id, action, note=""):
    ModerationDecision.objects.create(
        moderator=moderator, target_type=target_type, target_id=target_id, action=action, note=note
    )


@transaction.atomic
def approve_story(story, moderator):
    story = story_services.approve(story, moderator)
    _log(moderator, TargetType.STORY, story.id, DecisionAction.APPROVE)
    return story


@transaction.atomic
def reject_story(story, moderator, reason: str):
    story = story_services.reject(story, moderator, reason)
    _log(moderator, TargetType.STORY, story.id, DecisionAction.REJECT, reason)
    return story


@transaction.atomic
def decide_project(project: CommunityProject, moderator, approve: bool, note: str = ""):
    project = CommunityProject.objects.select_for_update().get(pk=project.pk)
    if project.status != ProjectStatus.PENDING_REVIEW:
        raise InvalidTransition()
    project.status = ProjectStatus.ACTIVE if approve else ProjectStatus.REJECTED
    project.save(update_fields=["status", "updated_at"])
    _log(moderator, TargetType.PROJECT, project.id, DecisionAction.APPROVE if approve else DecisionAction.REJECT, note)
    title = _("Your project is now active") if approve else _("Your project was not approved")
    notify(project.owner, NotificationType.PROJECT_STATUS, title, note, {"project_id": str(project.id)})
    return project


@transaction.atomic
def hide_comment(comment: Comment, moderator, note: str = ""):
    comment.is_hidden = True
    comment.save(update_fields=["is_hidden", "updated_at"])
    _log(moderator, TargetType.COMMENT, comment.id, DecisionAction.HIDE, note)
    return comment


def create_report(reporter, *, target_type, target_id, reason, details="") -> Report:
    model = TARGET_MODELS[target_type]
    if not model.objects.filter(pk=target_id).exists():
        raise ValidationError({"target_id": [_("The reported content does not exist.")]})
    report, _created = Report.objects.get_or_create(
        reporter=reporter, target_type=target_type, target_id=target_id, status=ReportStatus.OPEN,
        defaults={"reason": reason, "details": details},
    )
    return report


@transaction.atomic
def close_report(report: Report, moderator, resolved: bool, note: str = "") -> Report:
    if report.status != ReportStatus.OPEN:
        raise InvalidTransition()
    report.status = ReportStatus.RESOLVED if resolved else ReportStatus.DISMISSED
    report.save(update_fields=["status", "updated_at"])
    _log(
        moderator, report.target_type, report.target_id,
        DecisionAction.RESOLVE_REPORT if resolved else DecisionAction.DISMISS_REPORT, note,
    )
    return report
