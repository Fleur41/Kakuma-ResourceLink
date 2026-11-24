from django.db.models import Q
from drf_spectacular.utils import OpenApiParameter, extend_schema
from django.utils.translation import gettext as _
from rest_framework import permissions, status, viewsets
from rest_framework.exceptions import NotFound
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.accounts.models import Role

from .models import AlumniProfile, Visibility
from .serializers import AlumniOwnSerializer, AlumniPublicSerializer

UUID_REGEX = r"[0-9a-fA-F-]{36}"


def visible_alumni(user):
    qs = AlumniProfile.objects.select_related("user", "user__profile").filter(user__is_active=True)
    if user.is_authenticated:
        return qs.filter(Q(visibility__in=[Visibility.PUBLIC, Visibility.MEMBERS]) | Q(user=user))
    return qs.filter(visibility=Visibility.PUBLIC)


@extend_schema(tags=["alumni"])
class AlumniViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = AlumniPublicSerializer
    permission_classes = [permissions.AllowAny]
    lookup_value_regex = UUID_REGEX
    filterset_fields = ["country", "is_mentor", "willing_to_contribute"]
    search_fields = ["user__full_name", "profession", "organization", "country", "bio"]
    ordering_fields = ["created_at", "country"]

    def get_queryset(self):
        qs = visible_alumni(self.request.user)
        skill = self.request.query_params.get("skill")
        if skill:
            # JSON array containment: skills contains the exact tag.
            qs = qs.filter(skills__contains=[skill])
        return qs

    @extend_schema(parameters=[OpenApiParameter("skill", str)])
    def list(self, request, *args, **kwargs):
        return super().list(request, *args, **kwargs)


@extend_schema(tags=["alumni"])
class MyAlumniProfileView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = AlumniOwnSerializer

    def _get(self, request):
        return AlumniProfile.objects.filter(user=request.user).first()

    @extend_schema(responses=AlumniOwnSerializer)
    def get(self, request):
        profile = self._get(request)
        if not profile:
            raise NotFound(_("You have not created an alumni profile yet."))
        return Response(AlumniOwnSerializer(profile).data)

    def _save(self, request, partial):
        profile = self._get(request)
        serializer = AlumniOwnSerializer(profile, data=request.data, partial=partial and profile is not None)
        serializer.is_valid(raise_exception=True)
        created = profile is None
        profile = serializer.save(user=request.user) if created else serializer.save()
        # Alumni who register become "alumni" (mentors keep their role).
        if created and request.user.role == Role.MEMBER:
            request.user.role = Role.ALUMNI
            request.user.save(update_fields=["role"])
        return Response(AlumniOwnSerializer(profile).data, status=status.HTTP_201_CREATED if created else 200)

    @extend_schema(request=AlumniOwnSerializer, responses=AlumniOwnSerializer)
    def put(self, request):
        return self._save(request, partial=False)

    @extend_schema(request=AlumniOwnSerializer, responses=AlumniOwnSerializer)
    def patch(self, request):
        return self._save(request, partial=True)

    @extend_schema(responses={204: None})
    def delete(self, request):
        """Withdraw from the diaspora directory: the profile is deleted."""
        AlumniProfile.objects.filter(user=request.user).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
