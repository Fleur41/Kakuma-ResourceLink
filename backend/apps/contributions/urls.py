from rest_framework.routers import DefaultRouter

from .views import CommunityProjectViewSet

router = DefaultRouter()
router.register("projects", CommunityProjectViewSet, basename="project")
urlpatterns = router.urls
