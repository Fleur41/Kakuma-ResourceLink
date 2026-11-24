from django.urls import path
from rest_framework.routers import DefaultRouter

from .views import ChatView, ConversationViewSet

router = DefaultRouter()
router.register("ai/conversations", ConversationViewSet, basename="ai-conversation")
urlpatterns = [path("ai/chat/", ChatView.as_view(), name="ai-chat")] + router.urls
