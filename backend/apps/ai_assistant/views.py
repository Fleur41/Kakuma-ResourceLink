from drf_spectacular.utils import extend_schema
from rest_framework import mixins, permissions, viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.common.throttles import AIRateThrottle

from . import services
from .models import AIConversation
from .serializers import (
    AIMessageSerializer,
    ChatRequestSerializer,
    ChatResponseSerializer,
    ConversationDetailSerializer,
    ConversationSerializer,
)


class ChatView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [AIRateThrottle]

    @extend_schema(request=ChatRequestSerializer, responses={200: ChatResponseSerializer}, tags=["ai"])
    def post(self, request):
        s = ChatRequestSerializer(data=request.data)
        s.is_valid(raise_exception=True)
        conversation, reply = services.chat(user=request.user, **s.validated_data)
        return Response(
            {
                "conversation_id": str(conversation.id),
                "message": AIMessageSerializer(reply).data,
                "sources": reply.sources,
            }
        )


@extend_schema(tags=["ai"])
class ConversationViewSet(
    mixins.ListModelMixin, mixins.RetrieveModelMixin, mixins.DestroyModelMixin, viewsets.GenericViewSet
):
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = []

    def get_serializer_class(self):
        return ConversationDetailSerializer if self.action == "retrieve" else ConversationSerializer

    def get_queryset(self):
        if getattr(self, "swagger_fake_view", False):  # schema generation
            return AIConversation.objects.none()
        qs = AIConversation.objects.filter(user=self.request.user)
        return qs.prefetch_related("messages") if self.action == "retrieve" else qs
