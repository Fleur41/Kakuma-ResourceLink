from rest_framework.routers import DefaultRouter

from .views import CommentViewSet, StoryViewSet

router = DefaultRouter()
router.register("stories", StoryViewSet, basename="story")
router.register("comments", CommentViewSet, basename="comment")
urlpatterns = router.urls
