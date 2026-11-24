from rest_framework import serializers

from apps.accounts.serializers import PublicUserSerializer

from .models import HelpArea, MentorshipRequest, MentorshipStatus


class MentorshipRequestSerializer(serializers.ModelSerializer):
    mentee = PublicUserSerializer(read_only=True)
    mentor = PublicUserSerializer(read_only=True)
    mentor_id = serializers.UUIDField(write_only=True, help_text="Alumni profile id of the mentor")
    help_areas = serializers.ListField(
        child=serializers.ChoiceField(choices=HelpArea.choices), min_length=1, max_length=len(HelpArea.choices)
    )
    goal = serializers.CharField(min_length=3, max_length=200)
    message = serializers.CharField(min_length=10, max_length=2000)

    class Meta:
        model = MentorshipRequest
        fields = [
            "id", "mentee", "mentor", "mentor_id", "goal", "help_areas", "message", "status", "response_message",
            "responded_at", "created_at", "updated_at",
        ]
        read_only_fields = ["id", "status", "response_message", "responded_at", "created_at", "updated_at"]


class MentorshipStatusUpdateSerializer(serializers.Serializer):
    status = serializers.ChoiceField(
        choices=[MentorshipStatus.ACCEPTED, MentorshipStatus.DECLINED, MentorshipStatus.CANCELLED, MentorshipStatus.COMPLETED]
    )
    response_message = serializers.CharField(max_length=2000, required=False, allow_blank=True)
