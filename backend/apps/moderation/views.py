from drf_spectacular.utils import extend_schema
from rest_framework import mixins, permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from apps.common.permissions import IsModerator
from apps.contributions.models import CommunityProject, ProjectStatus
from apps.contributions.serializers import CommunityProjectSerializer
from apps.stories.models import Comment, Story, StoryStatus
from apps.stories.serializers import CommentSerializer, StoryDetailSerializer
from apps.stories.views import annotate_stories

from . import services
from .models import Report
from .serializers import NoteSerializer, RejectSerializer, ReportDecisionSerializer, ReportSerializer


@extend_schema(tags=["moderation"])
class StoryModerationViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, viewsets.GenericViewSet):
    """Review queue: stories waiting for approval (oldest first)."""

    permission_classes = [IsModerator]
    serializer_class = StoryDetailSerializer
    filter_backends = []

    def get_queryset(self):
        qs = Story.objects.filter(status=StoryStatus.PENDING_REVIEW).order_by("updated_at")
        return annotate_stories(qs, self.request.user)

    @extend_schema(request=None, responses=StoryDetailSerializer)
    @action(detail=True, methods=["post"])
    def approve(self, request, pk=None):
        services.approve_story(self.get_object(), request.user)
        return Response(self.get_serializer(annotate_stories(Story.objects.filter(pk=pk), request.user).get()).data)

    @extend_schema(request=RejectSerializer, responses=StoryDetailSerializer)
    @action(detail=True, methods=["post"])
    def reject(self, request, pk=None):
        s = RejectSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        services.reject_story(self.get_object(), request.user, s.validated_data["reason"])
        return Response(self.get_serializer(annotate_stories(Story.objects.filter(pk=pk), request.user).get()).data)


@extend_schema(tags=["moderation"])
class ProjectModerationViewSet(mixins.ListModelMixin, viewsets.GenericViewSet):
    permission_classes = [IsModerator]
    serializer_class = CommunityProjectSerializer
    filter_backends = []

    def get_queryset(self):
        return CommunityProject.objects.filter(status=ProjectStatus.PENDING_REVIEW).order_by("created_at")

    def _decide(self, request, pk, approve):
        s = NoteSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        project = services.decide_project(self.get_object(), request.user, approve, s.validated_data.get("note", ""))
        return Response(self.get_serializer(project).data)

    @extend_schema(request=NoteSerializer, responses=CommunityProjectSerializer)
    @action(detail=True, methods=["post"])
    def approve(self, request, pk=None):
        return self._decide(request, pk, True)

    @extend_schema(request=NoteSerializer, responses=CommunityProjectSerializer)
    @action(detail=True, methods=["post"])
    def reject(self, request, pk=None):
        return self._decide(request, pk, False)


@extend_schema(tags=["moderation"])
class CommentModerationViewSet(viewsets.GenericViewSet):
    permission_classes = [IsModerator]
    serializer_class = CommentSerializer
    queryset = Comment.objects.all()

    @extend_schema(request=NoteSerializer, responses=CommentSerializer)
    @action(detail=True, methods=["post"])
    def hide(self, request, pk=None):
        s = NoteSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        comment = services.hide_comment(self.get_object(), request.user, s.validated_data.get("note", ""))
        return Response(self.get_serializer(comment).data)


@extend_schema(tags=["moderation"])
class ReportViewSet(mixins.CreateModelMixin, mixins.ListModelMixin, viewsets.GenericViewSet):
    """Anyone signed in can report; only moderators can list and decide."""

    serializer_class = ReportSerializer
    filterset_fields = ["status", "target_type"]
    search_fields = []

    def get_permissions(self):
        if self.action == "create":
            return [permissions.IsAuthenticated()]
        return [IsModerator()]

    def get_queryset(self):
        return Report.objects.all()

    def create(self, request, *args, **kwargs):
        s = self.get_serializer(data=request.data)
        s.is_valid(raise_exception=True)
        report = services.create_report(request.user, **s.validated_data)
        return Response(self.get_serializer(report).data, status=status.HTTP_201_CREATED)

    @extend_schema(request=ReportDecisionSerializer, responses=ReportSerializer)
    @action(detail=True, methods=["post"])
    def decide(self, request, pk=None):
        s = ReportDecisionSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        report = services.close_report(
            self.get_object(), request.user, s.validated_data["resolved"], s.validated_data.get("note", "")
        )
        return Response(self.get_serializer(report).data)
