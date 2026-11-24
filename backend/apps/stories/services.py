"""Story lifecycle: draft → (publish) → pending_review → published | rejected."""
from django.conf import settings
from django.db import transaction
from django.utils import timezone
from django.utils.translation import gettext_lazy as _
from rest_framework.exceptions import ValidationError

from apps.common.exceptions import ConflictError, InvalidTransition
from apps.notifications.models import NotificationType
from apps.notifications.services import notify

from .models import Comment, Story, StoryStatus

EDITABLE_FIELDS_TRIGGERING_REVIEW = {"title", "body", "excerpt", "cover_image"}


def _publish_now(story: Story) -> None:
    story.status = StoryStatus.PUBLISHED
    story.published_at = story.published_at or timezone.now()
    story.rejection_reason = ""


@transaction.atomic
def submit_for_publication(story: Story, user) -> Story:
    story = Story.objects.select_for_update().get(pk=story.pk)
    if story.status == StoryStatus.PUBLISHED:
        raise ConflictError(_("This story is already published."))
    if story.status == StoryStatus.PENDING_REVIEW:
        raise ConflictError(_("This story is already waiting for review."))
    if not story.consent_to_publish:
        raise ValidationError({"consent_to_publish": [_("Please confirm you consent to publishing this story.")]})
    if not settings.STORIES_REQUIRE_REVIEW or user.is_moderator:
        _publish_now(story)
    else:
        story.status = StoryStatus.PENDING_REVIEW
        story.rejection_reason = ""
    story.save()
    return story


def after_author_edit(story: Story, changed_fields: set[str], user) -> Story:
    """Editing the content of a published story sends it back to review (when review is enabled)."""
    if (
        story.status == StoryStatus.PUBLISHED
        and settings.STORIES_REQUIRE_REVIEW
        and not user.is_moderator
        and changed_fields & EDITABLE_FIELDS_TRIGGERING_REVIEW
    ):
        story.status = StoryStatus.PENDING_REVIEW
        story.save(update_fields=["status", "updated_at"])
    return story


@transaction.atomic
def approve(story: Story, moderator) -> Story:
    story = Story.objects.select_for_update().get(pk=story.pk)
    if story.status != StoryStatus.PENDING_REVIEW:
        raise InvalidTransition()
    _publish_now(story)
    story.save()
    notify(
        story.author, NotificationType.STORY_PUBLISHED, _("Your story is live"),
        _("“%(title)s” has been published."), {"story_id": str(story.id)}, params={"title": story.title},
    )
    return story


@transaction.atomic
def reject(story: Story, moderator, reason: str) -> Story:
    story = Story.objects.select_for_update().get(pk=story.pk)
    if story.status != StoryStatus.PENDING_REVIEW:
        raise InvalidTransition()
    story.status = StoryStatus.REJECTED
    story.rejection_reason = reason
    story.save()
    notify(
        story.author, NotificationType.STORY_REJECTED, _("Your story needs changes"), reason,
        {"story_id": str(story.id)},
    )
    return story


def add_comment(story: Story, author, body: str) -> Comment:
    if story.status != StoryStatus.PUBLISHED:
        raise ConflictError(_("Comments are only available on published stories."))
    comment = Comment.objects.create(story=story, author=author, body=body)
    if story.author_id != author.id:
        notify(
            story.author, NotificationType.STORY_COMMENT, _("New comment on your story"),
            _("%(name)s commented on “%(title)s”."),
            {"story_id": str(story.id), "comment_id": str(comment.id)},
            params={"name": author.full_name, "title": story.title},
        )
    return comment
