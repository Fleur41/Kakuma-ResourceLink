import pytest
from django.utils import timezone

from apps.diaspora.models import AlumniProfile

pytestmark = pytest.mark.django_db
URL = "/api/v1/alumni/"
ME = "/api/v1/alumni/profile/"


def make_profile(user, **kw):
    data = dict(country="Germany", profession="Software engineer", skills=["Android", "Python"], consent_given_at=timezone.now())
    data.update(kw)
    return AlumniProfile.objects.create(user=user, **data)


def test_create_requires_consent(auth_client):
    client = auth_client()
    payload = {"country": "Canada", "profession": "Researcher", "skills": ["Research"], "is_mentor": True}
    res = client.put(ME, payload, format="json")
    assert res.status_code == 400 and "consent" in res.data["error"]["details"]
    res = client.put(ME, {**payload, "consent": True}, format="json")
    assert res.status_code == 201
    assert res.data["consent_given_at"]
    client.user.refresh_from_db()
    assert client.user.role == "alumni"
    assert client.get("/api/v1/auth/me/").data["has_alumni_profile"] is True


def test_get_missing_profile_404(auth_client):
    res = auth_client().get(ME)
    assert res.status_code == 404 and res.data["error"]["code"] == "not_found"


def test_patch_and_withdraw(auth_client):
    client = auth_client()
    make_profile(client.user)
    assert client.patch(ME, {"visibility": "public", "city": "Berlin"}, format="json").data["visibility"] == "public"
    assert client.delete(ME).status_code == 204
    assert not AlumniProfile.objects.exists()


def test_visibility_rules(api_client, auth_client, make_user):
    public = make_profile(make_user(), visibility="public")
    members = make_profile(make_user(), visibility="members")
    private = make_profile(make_user(), visibility="private")
    assert {r["id"] for r in api_client.get(URL).data["results"]} == {str(public.id)}
    viewer = auth_client()
    assert {r["id"] for r in viewer.get(URL).data["results"]} == {str(public.id), str(members.id)}
    assert viewer.get(f"{URL}{private.id}/").status_code == 404
    assert auth_client(private.user).get(f"{URL}{private.id}/").status_code == 200


def test_contact_email_and_city_only_with_consent(api_client, auth_client, make_user):
    shown = make_profile(make_user(), visibility="public", show_contact_email=True, city="Berlin", show_city=True)
    hidden = make_profile(make_user(), visibility="public", city="Toronto")
    viewer = auth_client()
    assert viewer.get(f"{URL}{shown.id}/").data["contact_email"] == shown.user.email
    assert viewer.get(f"{URL}{shown.id}/").data["city"] == "Berlin"
    assert api_client.get(f"{URL}{shown.id}/").data["contact_email"] is None  # anonymous never sees email
    data = viewer.get(f"{URL}{hidden.id}/").data
    assert data["contact_email"] is None and data["city"] is None
    assert "email" not in data


def test_search_and_filters(api_client, make_user):
    make_profile(make_user(full_name="Grace A"), visibility="public", country="Canada", profession="Public health researcher", skills=["Research"])
    make_profile(make_user(), visibility="public", is_mentor=True)
    assert api_client.get(URL, {"search": "health"}).data["count"] == 1
    assert api_client.get(URL, {"is_mentor": "true"}).data["count"] == 1
    assert api_client.get(URL, {"skill": "Python"}).data["count"] == 1
    assert api_client.get(URL, {"country": "Canada"}).data["count"] == 1
