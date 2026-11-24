from django.db.models import Q
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import mixins, permissions, status, viewsets
from rest_framework.response import Response

from apps.diaspora.views import visible_alumni

from . import services
from .models import MentorshipRequest
from .serializers import MentorshipRequestSerializer, MentorshipStatusUpdateSerializer


@extend_schema(tags=["mentorship"])
class MentorshipRequestViewSet(
    mixins.ListModelMixin, mixins.CreateModelMixin, mixins.RetrieveModelMixin, viewsets.GenericViewSet
):
    serializer_class = MentorshipRequestSerializer
    permission_classes = [permissions.IsAuthenticated]
    filterset_fields = ["status"]
    search_fields = []
    ordering_fields = ["created_at"]

    def get_queryset(self):
        if getattr(self, "swagger_fake_view", False):  # schema generation
            return MentorshipRequest.objects.none()
        user = self.request.user
        box = self.request.query_params.get("box")
        if box == "sent":
            scope = Q(mentee=user)
        elif box == "received":
            scope = Q(mentor=user)
        else:
            scope = Q(mentee=user) | Q(mentor=user)
        return MentorshipRequest.objects.filter(scope).select_related(
            "mentee", "mentee__profile", "mentee__alumni_profile", "mentor", "mentor__profile", "mentor__alumni_profile"
        )

    @extend_schema(parameters=[OpenApiParameter("box", str, enum=["sent", "received"])])
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        mentor_profile = get_object_or_404(visible_alumni(request.user), pk=data["mentor_id"])
        req = services.create_request(
            mentee=request.user, mentor_profile=mentor_profile, goal=data["goal"],
            help_areas=data["help_areas"], message=data["message"],
        )
        return Response(self.get_serializer(req).data, status=status.HTTP_201_CREATED)

    @extend_schema(request=MentorshipStatusUpdateSerializer, responses=MentorshipRequestSerializer)
    def partial_update(self, request, *args, **kwargs):
        req = self.get_object()
        serializer = MentorshipStatusUpdateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        req = services.change_status(
            req, request.user, serializer.validated_data["status"], serializer.validated_data.get("response_message", "")
        )
        return Response(self.get_serializer(req).data)
