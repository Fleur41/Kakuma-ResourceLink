from django.conf import settings
from rest_framework import serializers

from apps.accounts.models import Language

from .models import AIConversation, AIMessage

TASKS = ["chat", "explain_opportunity", "summarize_story", "translate"]


class ChatRequestSerializer(serializers.Serializer):
    message = serializers.CharField(min_length=1, max_length=settings.AI_MAX_INPUT_CHARS, trim_whitespace=True)
    conversation_id = serializers.UUIDField(required=False, allow_null=True)
    language = serializers.ChoiceField(choices=Language.choices, required=False)
    task = serializers.ChoiceField(choices=TASKS, default="chat")
    target_id = serializers.UUIDField(required=False, allow_null=True)
    target_language = serializers.ChoiceField(choices=Language.choices, required=False)

    def validate(self, attrs):
        if attrs.get("task") == "translate" and not attrs.get("target_language"):
            raise serializers.ValidationError({"target_language": ["Choose a language to translate into."]})
        return attrs


class SourceSerializer(serializers.Serializer):
    type = serializers.CharField()
    id = serializers.UUIDField()
    title = serializers.CharField()


class AIMessageSerializer(serializers.ModelSerializer):
    sources = SourceSerializer(many=True, read_only=True)

    class Meta:
        model = AIMessage
        fields = ["id", "role", "content", "sources", "created_at"]


class ChatResponseSerializer(serializers.Serializer):
    conversation_id = serializers.UUIDField()
    message = AIMessageSerializer()
    sources = SourceSerializer(many=True)


class ConversationSerializer(serializers.ModelSerializer):
    class Meta:
        model = AIConversation
        fields = ["id", "title", "language", "created_at", "updated_at"]


class ConversationDetailSerializer(ConversationSerializer):
    messages = AIMessageSerializer(many=True, read_only=True)

    class Meta(ConversationSerializer.Meta):
        fields = ConversationSerializer.Meta.fields + ["messages"]
