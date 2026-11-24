from drf_spectacular.utils import extend_schema
from rest_framework import viewsets

from apps.common.permissions import IsModeratorOrReadOnly, is_moderator

from .models import Service
from .serializers import ServiceSerializer


@extend_schema(tags=["services"])
class ServiceViewSet(viewsets.ModelViewSet):
    serializer_class = ServiceSerializer
    permission_classes = [IsModeratorOrReadOnly]
    filterset_fields = ["category", "is_verified"]
    search_fields = ["name", "organization", "description", "address"]
    ordering_fields = ["name", "created_at"]

    def get_queryset(self):
        qs = Service.objects.all()
        return qs if is_moderator(self.request.user) else qs.filter(is_active=True)
