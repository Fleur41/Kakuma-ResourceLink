from django.http import Http404
from django.db.models import Count, Exists, IntegerField, OuterRef, Q, Subquery, Value
from django.db.models.functions import Coalesce
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import mixins, permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response

from apps.common.permissions import IsOwnerOrReadOnly, is_moderator

from . import services
from .models import Comment, Story, StoryBookmark, StoryLike, StoryStatus
from .serializers import (
    StoryBookmarkStateSerializer,
    CommentSerializer,
    LikeStateSerializer,
    StoryDetailSerializer,
    StoryListSerializer,
)


def _count_subquery(model, **filters):
    sq = model.objects.filter(story=OuterRef("pk"), **filters).values("story").annotate(c=Count("pk")).values("c")
    return Coalesce(Subquery(sq, output_field=IntegerField()), Value(0))


def annotate_stories(qs, user):
    qs = qs.select_related("author", "author__profile", "author__alumni_profile").annotate(
        likes_count=_count_subquery(StoryLike),
        comments_count=_count_subquery(Comment, is_hidden=False),
    )
    if user.is_authenticated:
        return qs.annotate(
            is_liked=Exists(StoryLike.objects.filter(user=user, story=OuterRef("pk"))),
            is_bookmarked=Exists(StoryBookmark.objects.filter(user=user, story=OuterRef("pk"))),
        )
    return qs.annotate(is_liked=Value(False), is_bookmarked=Value(False))


@extend_schema(tags=["stories"])
class StoryViewSet(viewsets.ModelViewSet):
    permission_classes = [permissions.IsAuthenticatedOrReadOnly, IsOwnerOrReadOnly]
    parser_classes = [JSONParser, MultiPartParser, FormParser]
    owner_field = "author"
    filterset_fields = ["category", "language", "author"]
    search_fields = ["title", "excerpt", "body", "author__full_name"]
    ordering_fields = ["published_at", "created_at", "likes_count"]

    def get_serializer_class(self):
        return StoryListSerializer if self.action == "list" else StoryDetailSerializer

    def get_queryset(self):
        user = self.request.user
        params = self.request.query_params
        qs = Story.objects.all()
        if self.action == "list":
            if params.get("mine") in {"true", "1"} and user.is_authenticated:
                qs = qs.filter(author=user)
            else:
                qs = qs.filter(status=StoryStatus.PUBLISHED)
            if params.get("bookmarked") in {"true", "1"} and user.is_authenticated:
                qs = qs.filter(bookmarks__user=user)
        elif not is_moderator(user):
            visible = Q(status=StoryStatus.PUBLISHED)
            if user.is_authenticated:
                visible |= Q(author=user)
            qs = qs.filter(visible)
        return annotate_stories(qs, user)

    @extend_schema(parameters=[OpenApiParameter("mine", bool), OpenApiParameter("bookmarked", bool)])
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)

    def perform_create(self, serializer):
        serializer.save(author=self.request.user, status=StoryStatus.DRAFT)

    def perform_update(self, serializer):
        changed = set(serializer.validated_data.keys())
        old_cover = serializer.instance.cover_image.name if serializer.instance.cover_image else None
        story = serializer.save()
        if "cover_image" in changed and old_cover and old_cover != story.cover_image.name:
            story.cover_image.storage.delete(old_cover)
        services.after_author_edit(story, changed, self.request.user)

    def _fresh(self, story):
        return annotate_stories(Story.objects.filter(pk=story.pk), self.request.user).get()

    @extend_schema(request=None, responses=StoryDetailSerializer)
    @action(detail=True, methods=["post"])
    def publish(self, request, pk=None):
        story = self.get_object()
        if story.author_id != request.user.id:
            self.permission_denied(request)
        story = services.submit_for_publication(story, request.user)
        return Response(StoryDetailSerializer(self._fresh(story), context=self.get_serializer_context()).data)

    def _published_or_404(self):
        story = self.get_object()
        if story.status != StoryStatus.PUBLISHED:
            raise Http404
        return story

    @extend_schema(request=None, responses=LikeStateSerializer)
    @action(detail=True, methods=["post", "delete"], permission_classes=[permissions.IsAuthenticated])
    def like(self, request, pk=None):
        story = self._published_or_404()
        if request.method == "DELETE":
            StoryLike.objects.filter(user=request.user, story=story).delete()
            liked = False
        else:
            StoryLike.objects.get_or_create(user=request.user, story=story)
            liked = True
        return Response({"liked": liked, "likes_count": StoryLike.objects.filter(story=story).count()})

    @extend_schema(request=None, responses=StoryBookmarkStateSerializer)
    @action(detail=True, methods=["post", "delete"], permission_classes=[permissions.IsAuthenticated])
    def bookmark(self, request, pk=None):
        story = self._published_or_404()
        if request.method == "DELETE":
            StoryBookmark.objects.filter(user=request.user, story=story).delete()
            return Response({"bookmarked": False})
        StoryBookmark.objects.get_or_create(user=request.user, story=story)
        return Response({"bookmarked": True})

    @extend_schema(methods=["GET"], responses=CommentSerializer(many=True))
    @extend_schema(methods=["POST"], request=CommentSerializer, responses={201: CommentSerializer})
    @action(detail=True, methods=["get", "post"], permission_classes=[permissions.IsAuthenticatedOrReadOnly])
    def comments(self, request, pk=None):
        story = self._published_or_404()
        if request.method == "POST":
            serializer = CommentSerializer(data=request.data, context=self.get_serializer_context())
            serializer.is_valid(raise_exception=True)
            comment = services.add_comment(story, request.user, serializer.validated_data["body"])
            return Response(CommentSerializer(comment, context=self.get_serializer_context()).data, status=201)
        qs = story.comments.filter(is_hidden=False).select_related("author", "author__profile", "author__alumni_profile")
        page = self.paginate_queryset(qs)
        data = CommentSerializer(page, many=True, context=self.get_serializer_context()).data
        return self.get_paginated_response(data)


@extend_schema(tags=["stories"])
class CommentViewSet(mixins.DestroyModelMixin, viewsets.GenericViewSet):
    """Delete own comment (moderators can delete any)."""

    permission_classes = [permissions.IsAuthenticated]
    serializer_class = CommentSerializer

    def get_queryset(self):
        if getattr(self, "swagger_fake_view", False):  # schema generation
            return Comment.objects.none()
        qs = Comment.objects.all()
        return qs if is_moderator(self.request.user) else qs.filter(author=self.request.user)

    def destroy(self, request, *args, **kwargs):
        self.get_object().delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
