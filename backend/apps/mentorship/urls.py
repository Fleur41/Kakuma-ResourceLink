from rest_framework.routers import DefaultRouter

from .views import MentorshipRequestViewSet

router = DefaultRouter()
router.register("mentorship/requests", MentorshipRequestViewSet, basename="mentorship-request")
urlpatterns = router.urls
