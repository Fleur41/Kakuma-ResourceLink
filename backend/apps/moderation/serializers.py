from rest_framework import serializers

from .models import Report


class ReportSerializer(serializers.ModelSerializer):
    class Meta:
        model = Report
        fields = ["id", "target_type", "target_id", "reason", "details", "status", "created_at"]
        read_only_fields = ["id", "status", "created_at"]


class RejectSerializer(serializers.Serializer):
    reason = serializers.CharField(min_length=3, max_length=1000)


class NoteSerializer(serializers.Serializer):
    note = serializers.CharField(max_length=1000, required=False, allow_blank=True)


class ReportDecisionSerializer(NoteSerializer):
    resolved = serializers.BooleanField()
