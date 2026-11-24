from django.db.models import Q
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response

from apps.common.permissions import IsOwnerOrReadOnly, is_moderator

from . import services
from .models import CommunityProject, ProjectStatus
from .serializers import CommunityProjectSerializer, ContributionSerializer

PUBLIC_STATUSES = [ProjectStatus.ACTIVE, ProjectStatus.COMPLETED]


@extend_schema(tags=["projects"])
class CommunityProjectViewSet(viewsets.ModelViewSet):
    serializer_class = CommunityProjectSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly, IsOwnerOrReadOnly]
    parser_classes = [JSONParser, MultiPartParser, FormParser]
    filterset_fields = ["category", "status"]
    search_fields = ["title", "summary", "description", "location"]
    ordering_fields = ["created_at", "title"]

    def get_queryset(self):
        user = self.request.user
        qs = CommunityProject.objects.select_related("owner", "owner__profile", "owner__alumni_profile")
        if self.request.query_params.get("mine") in {"true", "1"} and user.is_authenticated:
            return qs.filter(owner=user)
        if is_moderator(user) and self.action != "list":
            return qs
        visible = Q(status__in=PUBLIC_STATUSES)
        if user.is_authenticated:
            visible |= Q(owner=user)
        return qs.filter(visible)

    @extend_schema(parameters=[OpenApiParameter("mine", bool)])
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)

    def perform_create(self, serializer):
        serializer.save(owner=self.request.user, status=services.initial_project_status(self.request.user))

    @extend_schema(methods=["GET"], responses=ContributionSerializer(many=True))
    @extend_schema(methods=["POST"], request=ContributionSerializer, responses={201: ContributionSerializer})
    @action(detail=True, methods=["get", "post"], permission_classes=[permissions.IsAuthenticated])
    def contributions(self, request, pk=None):
        """Pledge support. Listing shows everything to the owner/moderators and only your own pledges to others;
        the public sees aggregate totals via `contribution_summary`."""
        project = self.get_object()
        if request.method == "POST":
            serializer = ContributionSerializer(data=request.data, context=self.get_serializer_context())
            serializer.is_valid(raise_exception=True)
            contribution = services.add_contribution(project, request.user, **serializer.validated_data)
            return Response(ContributionSerializer(contribution).data, status=status.HTTP_201_CREATED)
        qs = project.contributions.all()
        if project.owner_id != request.user.id and not is_moderator(request.user):
            qs = qs.filter(contributor=request.user)
        qs = qs.select_related("contributor", "contributor__profile", "contributor__alumni_profile")
        page = self.paginate_queryset(qs)
        return self.get_paginated_response(ContributionSerializer(page, many=True).data)
