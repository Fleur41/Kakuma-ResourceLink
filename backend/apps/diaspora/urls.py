from django.urls import path
from rest_framework.routers import DefaultRouter

from .views import AlumniViewSet, MyAlumniProfileView

router = DefaultRouter()
router.register("alumni", AlumniViewSet, basename="alumni")
urlpatterns = [path("alumni/profile/", MyAlumniProfileView.as_view(), name="alumni-my-profile")] + router.urls
