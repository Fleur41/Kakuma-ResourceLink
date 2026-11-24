import pytest

from apps.services.models import Service

pytestmark = pytest.mark.django_db
URL = "/api/v1/services/"


def test_list_filter_and_hide_inactive(api_client):
    Service.objects.create(name="Sample clinic", category="healthcare", description="Sample", is_verified=True)
    Service.objects.create(name="Sample legal aid desk", category="legal", description="Sample")
    Service.objects.create(name="Closed office", category="community", description="x", is_active=False)
    assert api_client.get(URL).data["count"] == 2
    assert api_client.get(URL, {"category": "healthcare"}).data["results"][0]["name"] == "Sample clinic"
    assert api_client.get(URL, {"is_verified": "true"}).data["count"] == 1
    assert api_client.get(URL, {"search": "legal"}).data["count"] == 1


def test_write_requires_moderator(auth_client, moderator):
    payload = {"name": "Desk", "category": "community", "description": "Sample"}
    assert auth_client().post(URL, payload, format="json").status_code == 403
    assert auth_client(moderator).post(URL, payload, format="json").status_code == 201
