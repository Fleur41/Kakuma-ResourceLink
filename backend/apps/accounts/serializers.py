from django.contrib.auth import authenticate
from django.contrib.auth import password_validation
from django.contrib.auth.tokens import default_token_generator
from django.utils.translation import gettext_lazy as _
from rest_framework import serializers

from .models import SELF_ASSIGNABLE_ROLES, Language, Profile, Role, User
from .services import user_from_reset_uid


class ProfileSerializer(serializers.ModelSerializer):
    avatar = serializers.ImageField(read_only=True)

    class Meta:
        model = Profile
        fields = ["bio", "location", "avatar"]


class UserSerializer(serializers.ModelSerializer):
    profile = ProfileSerializer(required=False)
    has_alumni_profile = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["id", "email", "full_name", "role", "preferred_language", "date_joined", "profile", "has_alumni_profile"]
        read_only_fields = ["id", "email", "role", "date_joined", "has_alumni_profile"]

    def get_has_alumni_profile(self, obj) -> bool:
        return hasattr(obj, "alumni_profile")

    def update(self, instance, validated_data):
        profile_data = validated_data.pop("profile", None)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        if profile_data:
            for attr, value in profile_data.items():
                setattr(instance.profile, attr, value)
            instance.profile.save()
        return instance


class PublicUserSerializer(serializers.ModelSerializer):
    """Minimal author card. Never includes email or private profile fields."""

    avatar = serializers.ImageField(source="profile.avatar", read_only=True)
    country = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["id", "full_name", "avatar", "country"]

    def get_country(self, obj) -> str | None:
        alumni = getattr(obj, "alumni_profile", None)
        if alumni and alumni.visibility == "public":
            return alumni.country or None
        return None


class RegisterSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8, max_length=128, style={"input_type": "password"})
    full_name = serializers.CharField(max_length=150, min_length=2)
    role = serializers.ChoiceField(choices=[(r.value, r.label) for r in SELF_ASSIGNABLE_ROLES], default=Role.MEMBER)
    preferred_language = serializers.ChoiceField(choices=Language.choices, default=Language.EN)

    def validate_email(self, value: str) -> str:
        value = value.strip().lower()
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError(_("An account with this email already exists."))
        return value

    def validate_full_name(self, value: str) -> str:
        return value.strip()

    def validate(self, attrs):
        candidate = User(email=attrs["email"], full_name=attrs["full_name"])
        password_validation.validate_password(attrs["password"], user=candidate)
        return attrs


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, style={"input_type": "password"})

    def validate(self, attrs):
        user = authenticate(
            request=self.context.get("request"), email=attrs["email"].strip().lower(), password=attrs["password"]
        )
        if not user:
            from rest_framework.exceptions import AuthenticationFailed

            raise AuthenticationFailed(_("Incorrect email or password."), code="authentication_failed")
        attrs["user"] = user
        return attrs


class TokenPairSerializer(serializers.Serializer):
    access = serializers.CharField()
    refresh = serializers.CharField()


class AuthResponseSerializer(serializers.Serializer):
    user = UserSerializer()
    tokens = TokenPairSerializer()


class LogoutSerializer(serializers.Serializer):
    refresh = serializers.CharField()


class PasswordResetRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()


class PasswordResetConfirmSerializer(serializers.Serializer):
    uid = serializers.CharField()
    token = serializers.CharField()
    new_password = serializers.CharField(write_only=True, min_length=8, max_length=128)

    def validate(self, attrs):
        user = user_from_reset_uid(attrs["uid"])
        if not user or not default_token_generator.check_token(user, attrs["token"]):
            raise serializers.ValidationError({"token": [_("This reset link is invalid or has expired.")]})
        password_validation.validate_password(attrs["new_password"], user=user)
        attrs["user"] = user
        return attrs


class ChangePasswordSerializer(serializers.Serializer):
    old_password = serializers.CharField(write_only=True)
    new_password = serializers.CharField(write_only=True, min_length=8, max_length=128)

    def validate_old_password(self, value):
        if not self.context["request"].user.check_password(value):
            raise serializers.ValidationError(_("Current password is incorrect."))
        return value

    def validate(self, attrs):
        password_validation.validate_password(attrs["new_password"], user=self.context["request"].user)
        return attrs


class AvatarSerializer(serializers.ModelSerializer):
    class Meta:
        model = Profile
        fields = ["avatar"]
        # Not redeclared, so the model's size/type validators still apply.
        extra_kwargs = {"avatar": {"required": True, "allow_null": False}}
