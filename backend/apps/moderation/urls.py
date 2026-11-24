from rest_framework.routers import DefaultRouter

from . import views

router = DefaultRouter()
router.register("moderation/stories", views.StoryModerationViewSet, basename="moderation-story")
router.register("moderation/projects", views.ProjectModerationViewSet, basename="moderation-project")
router.register("moderation/comments", views.CommentModerationViewSet, basename="moderation-comment")
router.register("moderation/reports", views.ReportViewSet, basename="moderation-report")
urlpatterns = router.urls
