import uuid

from django.conf import settings
from django.db import models
from django.utils.text import slugify
from django.utils.translation import gettext_lazy as _

from apps.accounts.models import Language
from apps.common.models import TimeStampedModel, UUIDModel
from apps.common.text import reading_time_minutes
from apps.common.uploads import randomized_upload_to
from apps.common.validators import IMAGE_VALIDATORS


class StoryStatus(models.TextChoices):
    DRAFT = "draft", _("Draft")
    PENDING_REVIEW = "pending_review", _("In review")
    PUBLISHED = "published", _("Published")
    REJECTED = "rejected", _("Needs changes")
    ARCHIVED = "archived", _("Archived")


class StoryCategory(models.TextChoices):
    EDUCATION = "education", _("Education")
    CAREER = "career", _("Career")
    RESETTLEMENT = "resettlement", _("Resettlement")
    ENTREPRENEURSHIP = "entrepreneurship", _("Entrepreneurship")
    INSPIRATION = "inspiration", _("Inspiration")
    ADVICE = "advice", _("Advice")
    COMMUNITY = "community", _("Community")


class Story(UUIDModel):
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="stories")
    title = models.CharField(max_length=200)
    slug = models.SlugField(max_length=230, unique=True, editable=False)
    excerpt = models.CharField(max_length=300, blank=True)
    # CommonMark Markdown. Clients render with raw HTML disabled.
    body = models.TextField(max_length=50_000)
    cover_image = models.ImageField(
        upload_to=randomized_upload_to("stories/covers"), blank=True, validators=IMAGE_VALIDATORS
    )
    category = models.CharField(max_length=20, choices=StoryCategory.choices, default=StoryCategory.INSPIRATION)
    language = models.CharField(max_length=5, choices=Language.choices, default=Language.EN)
    status = models.CharField(max_length=20, choices=StoryStatus.choices, default=StoryStatus.DRAFT)
    # Explicit author consent is required before a story can leave draft.
    consent_to_publish = models.BooleanField(default=False)
    rejection_reason = models.TextField(blank=True)
    reading_time = models.PositiveSmallIntegerField(default=1)
    published_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-published_at", "-created_at"]
        verbose_name_plural = "stories"
        indexes = [
            models.Index(fields=["status", "-published_at"]),
            models.Index(fields=["author", "status"]),
        ]

    def __str__(self) -> str:
        return self.title

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = f"{slugify(self.title)[:200] or 'story'}-{uuid.uuid4().hex[:8]}"
        self.reading_time = reading_time_minutes(self.body)
        super().save(*args, **kwargs)


class Comment(UUIDModel):
    story = models.ForeignKey(Story, on_delete=models.CASCADE, related_name="comments")
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="story_comments")
    body = models.TextField(max_length=2000)
    is_hidden = models.BooleanField(default=False)

    class Meta:
        ordering = ["created_at"]
        indexes = [models.Index(fields=["story", "created_at"])]


class StoryLike(TimeStampedModel):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="story_likes")
    story = models.ForeignKey(Story, on_delete=models.CASCADE, related_name="likes")

    class Meta:
        constraints = [models.UniqueConstraint(fields=["user", "story"], name="unique_story_like")]


class StoryBookmark(TimeStampedModel):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="story_bookmarks")
    story = models.ForeignKey(Story, on_delete=models.CASCADE, related_name="bookmarks")

    class Meta:
        constraints = [models.UniqueConstraint(fields=["user", "story"], name="unique_story_bookmark")]
