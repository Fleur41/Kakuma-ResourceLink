from rest_framework import serializers

from .models import Opportunity


class OpportunitySerializer(serializers.ModelSerializer):
    is_bookmarked = serializers.BooleanField(read_only=True, default=False)
    requirements = serializers.ListField(child=serializers.CharField(max_length=300), required=False, max_length=30)

    class Meta:
        model = Opportunity
        fields = [
            "id", "title", "category", "organization", "summary", "description", "requirements", "location",
            "is_remote", "deadline", "application_url", "language", "status", "is_verified", "is_bookmarked",
            "created_at", "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]


class BookmarkStateSerializer(serializers.Serializer):
    bookmarked = serializers.BooleanField()
