from django.db.models import Exists, OuterRef, Value
from drf_spectacular.utils import extend_schema
from rest_framework import permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from apps.common.permissions import IsModeratorOrReadOnly, is_moderator

from .filters import OpportunityFilter
from .models import Opportunity, OpportunityBookmark, OpportunityStatus
from .serializers import BookmarkStateSerializer, OpportunitySerializer


@extend_schema(tags=["opportunities"])
class OpportunityViewSet(viewsets.ModelViewSet):
    serializer_class = OpportunitySerializer
    permission_classes = [IsModeratorOrReadOnly]
    filterset_class = OpportunityFilter
    search_fields = ["title", "organization", "summary", "description", "location"]
    ordering_fields = ["deadline", "created_at", "title"]

    def get_queryset(self):
        user = self.request.user
        qs = Opportunity.objects.all()
        if not is_moderator(user):
            qs = qs.filter(status=OpportunityStatus.PUBLISHED)
        if user.is_authenticated:
            qs = qs.annotate(
                is_bookmarked=Exists(OpportunityBookmark.objects.filter(user=user, opportunity=OuterRef("pk")))
            )
        else:
            qs = qs.annotate(is_bookmarked=Value(False))
        return qs

    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)

    @extend_schema(request=None, responses={200: BookmarkStateSerializer, 201: BookmarkStateSerializer})
    @action(detail=True, methods=["post", "delete"], permission_classes=[permissions.IsAuthenticated])
    def bookmark(self, request, pk=None):
        opportunity = self.get_object()
        if request.method == "DELETE":
            OpportunityBookmark.objects.filter(user=request.user, opportunity=opportunity).delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        _obj, created = OpportunityBookmark.objects.get_or_create(user=request.user, opportunity=opportunity)
        return Response({"bookmarked": True}, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)
