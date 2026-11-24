import re

from rest_framework import serializers

from apps.accounts.serializers import PublicUserSerializer

from .models import Comment, Story

_MD_SYNTAX = re.compile(r"[#>*_`~\[\]()!-]+")


def make_excerpt(body: str, limit: int = 280) -> str:
    text = " ".join(_MD_SYNTAX.sub(" ", body or "").split())
    return text if len(text) <= limit else text[: limit - 1].rsplit(" ", 1)[0] + "…"


class StoryListSerializer(serializers.ModelSerializer):
    author = PublicUserSerializer(read_only=True)
    likes_count = serializers.IntegerField(read_only=True, default=0)
    comments_count = serializers.IntegerField(read_only=True, default=0)
    is_liked = serializers.BooleanField(read_only=True, default=False)
    is_bookmarked = serializers.BooleanField(read_only=True, default=False)

    class Meta:
        model = Story
        fields = [
            "id", "title", "slug", "excerpt", "cover_image", "category", "language", "status", "author",
            "reading_time", "likes_count", "comments_count", "is_liked", "is_bookmarked", "published_at",
            "created_at", "updated_at",
        ]
        read_only_fields = fields


class StoryDetailSerializer(StoryListSerializer):
    class Meta(StoryListSerializer.Meta):
        fields = StoryListSerializer.Meta.fields + ["body", "consent_to_publish", "rejection_reason"]
        read_only_fields = [
            "id", "slug", "status", "author", "reading_time", "likes_count", "comments_count", "is_liked",
            "is_bookmarked", "published_at", "created_at", "updated_at", "rejection_reason",
        ]
        extra_kwargs = {"excerpt": {"required": False}}

    def validate_title(self, value: str) -> str:
        value = value.strip()
        if len(value) < 3:
            raise serializers.ValidationError("Title must be at least 3 characters.")
        return value

    def validate(self, attrs):
        # Derive the card excerpt from the body unless the author wrote one.
        if "body" in attrs and not attrs.get("excerpt"):
            attrs["excerpt"] = make_excerpt(attrs["body"])
        return attrs


class CommentSerializer(serializers.ModelSerializer):
    author = PublicUserSerializer(read_only=True)
    body = serializers.CharField(min_length=1, max_length=2000, trim_whitespace=True)

    class Meta:
        model = Comment
        fields = ["id", "story", "author", "body", "created_at"]
        read_only_fields = ["id", "story", "author", "created_at"]


class LikeStateSerializer(serializers.Serializer):
    liked = serializers.BooleanField()
    likes_count = serializers.IntegerField()


class StoryBookmarkStateSerializer(serializers.Serializer):
    bookmarked = serializers.BooleanField()
