from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.http import JsonResponse
from django.urls import include, path
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView


def health(_request):
    return JsonResponse({"status": "ok"})


api_v1 = [
    path("auth/", include("apps.accounts.urls")),
    path("", include("apps.opportunities.urls")),
    path("", include("apps.services.urls")),
    path("", include("apps.stories.urls")),
    path("", include("apps.diaspora.urls")),
    path("", include("apps.mentorship.urls")),
    path("", include("apps.contributions.urls")),
    path("", include("apps.notifications.urls")),
    path("", include("apps.moderation.urls")),
    path("", include("apps.ai_assistant.urls")),
    path("schema/", SpectacularAPIView.as_view(), name="schema"),
    path("docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
]

urlpatterns = [
    path("admin/", admin.site.urls),
    path("health/", health, name="health"),
    path("api/v1/", include(api_v1)),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
