import pytest

pytestmark = pytest.mark.django_db


def test_404_uses_error_envelope(api_client):
    res = api_client.get("/api/v1/opportunities/00000000-0000-0000-0000-000000000000/")
    assert res.status_code == 404
    assert res.data == {"error": {"code": "not_found", "message": res.data["error"]["message"], "details": {}}}


def test_health_and_schema(api_client):
    assert api_client.get("/health/").json() == {"status": "ok"}
    assert api_client.get("/api/v1/schema/").status_code == 200


def test_messages_localized_by_accept_language(api_client):
    res = api_client.get("/api/v1/auth/me/", HTTP_ACCEPT_LANGUAGE="sw")
    assert res.status_code == 401
