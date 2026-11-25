from django.utils.translation import gettext as _
from drf_spectacular.utils import extend_schema
from rest_framework import generics, permissions, status
from rest_framework.exceptions import ValidationError
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.exceptions import TokenError
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenRefreshView

from apps.common.throttles import AuthRateThrottle

from . import services
from .serializers import (
    AuthResponseSerializer,
    AvatarSerializer,
    ChangePasswordSerializer,
    LoginSerializer,
    LogoutSerializer,
    PasswordResetConfirmSerializer,
    PasswordResetRequestSerializer,
    RegisterSerializer,
    UserSerializer,
)


class PublicAuthView(APIView):
    """Unauthenticated endpoint that still answers 401 (not 403) on bad credentials."""

    permission_classes = [permissions.AllowAny]
    authentication_classes = []
    throttle_classes = [AuthRateThrottle]

    def get_authenticate_header(self, request):
        return 'Bearer realm="api"'


def _auth_payload(user, request):
    return {"user": UserSerializer(user, context={"request": request}).data, "tokens": services.issue_tokens(user)}


class RegisterView(PublicAuthView):
    @extend_schema(request=RegisterSerializer, responses={201: AuthResponseSerializer}, tags=["auth"])
    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = services.register_user(**serializer.validated_data)
        return Response(_auth_payload(user, request), status=status.HTTP_201_CREATED)


class LoginView(PublicAuthView):
    @extend_schema(request=LoginSerializer, responses={200: AuthResponseSerializer}, tags=["auth"])
    def post(self, request):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        return Response(_auth_payload(serializer.validated_data["user"], request))


@extend_schema(tags=["auth"])
class RefreshView(TokenRefreshView):
    """Rotates the refresh token; the previous one is blacklisted."""

    throttle_classes = [AuthRateThrottle]


class LogoutView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    @extend_schema(request=LogoutSerializer, responses={204: None}, tags=["auth"])
    def post(self, request):
        serializer = LogoutSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            token = RefreshToken(serializer.validated_data["refresh"])
        except TokenError:
            raise ValidationError({"refresh": [_("Invalid or expired refresh token.")]})
        if str(token.get("user_id")) != str(request.user.id):
            raise ValidationError({"refresh": [_("Invalid or expired refresh token.")]})
        token.blacklist()
        return Response(status=status.HTTP_204_NO_CONTENT)


@extend_schema(tags=["auth"])
class MeView(generics.RetrieveUpdateAPIView):
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]
    http_method_names = ["get", "patch", "options"]

    def get_object(self):
        return self.request.user


class AvatarUploadView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    parser_classes = [MultiPartParser, FormParser]

    @extend_schema(request={"multipart/form-data": AvatarSerializer}, responses={200: UserSerializer}, tags=["auth"])
    def post(self, request):
        profile = request.user.profile
        serializer = AvatarSerializer(profile, data=request.data)
        serializer.is_valid(raise_exception=True)
        if profile.avatar:
            profile.avatar.delete(save=False)
        serializer.save()
        return Response(UserSerializer(request.user, context={"request": request}).data)

    @extend_schema(request=None, responses={200: UserSerializer}, tags=["auth"])
    def delete(self, request):
        """Removes the profile photo (deletes the file). Safe to call when there is no photo."""
        profile = request.user.profile
        if profile.avatar:
            profile.avatar.delete(save=False)
            profile.avatar = None
            profile.save(update_fields=["avatar"])
        return Response(UserSerializer(request.user, context={"request": request}).data)


class PasswordResetRequestView(PublicAuthView):
    @extend_schema(request=PasswordResetRequestSerializer, responses={202: None}, tags=["auth"])
    def post(self, request):
        serializer = PasswordResetRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        services.send_password_reset(serializer.validated_data["email"])
        return Response(
            {"detail": _("If an account exists for this email, a reset link has been sent.")},
            status=status.HTTP_202_ACCEPTED,
        )


class PasswordResetConfirmView(PublicAuthView):
    @extend_schema(request=PasswordResetConfirmSerializer, responses={204: None}, tags=["auth"])
    def post(self, request):
        serializer = PasswordResetConfirmSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        services.set_new_password(serializer.validated_data["user"], serializer.validated_data["new_password"])
        return Response(status=status.HTTP_204_NO_CONTENT)


class ChangePasswordView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    @extend_schema(request=ChangePasswordSerializer, responses={204: None}, tags=["auth"])
    def post(self, request):
        serializer = ChangePasswordSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        services.set_new_password(request.user, serializer.validated_data["new_password"])
        return Response(status=status.HTTP_204_NO_CONTENT)
