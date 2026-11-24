from rest_framework import serializers

from .models import Service


class ServiceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Service
        fields = [
            "id", "name", "category", "organization", "description", "phone", "email", "website", "address",
            "latitude", "longitude", "opening_hours", "is_verified", "last_verified_at", "is_active",
            "created_at", "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]
