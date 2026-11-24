from rest_framework import serializers

from apps.accounts.serializers import PublicUserSerializer

from .models import CommunityProject, Contribution, ContributionType
from .services import contribution_summary


class ContributionSummarySerializer(serializers.Serializer):
    total_contributions = serializers.IntegerField()
    pledged_amount = serializers.DecimalField(max_digits=14, decimal_places=2)
    currency = serializers.CharField()
    by_type = serializers.DictField(child=serializers.IntegerField())
    note = serializers.CharField()


class CommunityProjectSerializer(serializers.ModelSerializer):
    owner = PublicUserSerializer(read_only=True)
    accepted_contribution_types = serializers.ListField(
        child=serializers.ChoiceField(choices=ContributionType.choices), required=False
    )
    contribution_summary = serializers.SerializerMethodField()

    class Meta:
        model = CommunityProject
        fields = [
            "id", "owner", "title", "summary", "description", "category", "location", "cover_image", "status",
            "accepted_contribution_types", "funding_goal", "currency", "contribution_summary", "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "owner", "status", "created_at", "updated_at"]

    def get_contribution_summary(self, obj) -> ContributionSummarySerializer:
        return contribution_summary(obj)

    def validate_currency(self, value: str) -> str:
        value = value.upper()
        if len(value) != 3 or not value.isalpha():
            raise serializers.ValidationError("Use a 3-letter currency code, e.g. USD or KES.")
        return value


class ContributionSerializer(serializers.ModelSerializer):
    contributor = PublicUserSerializer(read_only=True)
    amount = serializers.DecimalField(max_digits=12, decimal_places=2, required=False, allow_null=True, min_value=0)
    description = serializers.CharField(min_length=3, max_length=1000)

    class Meta:
        model = Contribution
        fields = ["id", "project", "contributor", "type", "description", "amount", "currency", "status", "created_at"]
        read_only_fields = ["id", "project", "contributor", "currency", "status", "created_at"]
