from django.utils import timezone
from django.utils.translation import gettext_lazy as _
from rest_framework import serializers

from .models import AlumniProfile

def _tag_list():
    return serializers.ListField(child=serializers.CharField(max_length=50), required=False, max_length=20)


class AlumniPublicSerializer(serializers.ModelSerializer):
    """What other people can see — respects the alumnus's own privacy choices."""

    user_id = serializers.UUIDField(source="user.id", read_only=True)
    full_name = serializers.CharField(source="user.full_name", read_only=True)
    avatar = serializers.ImageField(source="user.profile.avatar", read_only=True)
    city = serializers.SerializerMethodField()
    contact_email = serializers.SerializerMethodField()

    class Meta:
        model = AlumniProfile
        fields = [
            "id", "user_id", "full_name", "avatar", "country", "city", "profession", "organization", "skills",
            "bio", "is_mentor", "mentorship_areas", "willing_to_contribute", "contribution_interests",
            "contact_email",
        ]

    def _viewer(self):
        request = self.context.get("request")
        return request.user if request else None

    def get_city(self, obj) -> str | None:
        return obj.city if obj.show_city and obj.city else None

    def get_contact_email(self, obj) -> str | None:
        viewer = self._viewer()
        # Contact details are only for signed-in members and only with explicit consent.
        if obj.show_contact_email and viewer and viewer.is_authenticated:
            return obj.user.email
        return None


class AlumniOwnSerializer(serializers.ModelSerializer):
    """The owner's full view/edit form, including privacy controls."""

    skills = _tag_list()
    mentorship_areas = _tag_list()
    contribution_interests = _tag_list()
    consent = serializers.BooleanField(write_only=True, required=False)
    full_name = serializers.CharField(source="user.full_name", read_only=True)

    class Meta:
        model = AlumniProfile
        fields = [
            "id", "full_name", "country", "city", "profession", "organization", "skills", "bio", "is_mentor",
            "mentorship_areas", "willing_to_contribute", "contribution_interests", "visibility",
            "show_contact_email", "show_city", "consent", "consent_given_at", "created_at", "updated_at",
        ]
        read_only_fields = ["id", "consent_given_at", "created_at", "updated_at"]

    def validate(self, attrs):
        if self.instance is None and not attrs.get("consent"):
            raise serializers.ValidationError(
                {"consent": [_("Please confirm you agree to share this profile with the community.")]}
            )
        return attrs

    def create(self, validated_data):
        validated_data.pop("consent", None)
        return AlumniProfile.objects.create(consent_given_at=timezone.now(), **validated_data)

    def update(self, instance, validated_data):
        validated_data.pop("consent", None)
        return super().update(instance, validated_data)
