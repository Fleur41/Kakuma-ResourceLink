import pytest
from django.core import mail
from django.urls import reverse
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken

from apps.accounts.models import Profile, Role, User
from conftest import PASSWORD

pytestmark = pytest.mark.django_db

REGISTER = "/api/v1/auth/register/"
LOGIN = "/api/v1/auth/login/"
REFRESH = "/api/v1/auth/token/refresh/"
LOGOUT = "/api/v1/auth/logout/"
ME = "/api/v1/auth/me/"


def register(client, **overrides):
    payload = {
        "email": "Amina@Example.test", "password": PASSWORD, "full_name": "Amina Test",
        "role": "member", "preferred_language": "sw",
    }
    payload.update(overrides)
    return client.post(REGISTER, payload, format="json")


class TestRegistration:
    def test_register_returns_user_and_tokens(self, api_client):
        res = register(api_client)
        assert res.status_code == 201
        assert res.data["user"]["email"] == "amina@example.test"  # normalised
        assert res.data["user"]["preferred_language"] == "sw"
        assert set(res.data["tokens"]) == {"access", "refresh"}
        user = User.objects.get(email="amina@example.test")
        assert Profile.objects.filter(user=user).exists()
        assert user.check_password(PASSWORD)

    def test_duplicate_email_case_insensitive(self, api_client):
        register(api_client)
        res = register(api_client, email="AMINA@example.test")
        assert res.status_code == 400
        assert res.data["error"]["code"] == "validation_error"
        assert "email" in res.data["error"]["details"]

    def test_weak_password_rejected(self, api_client):
        res = register(api_client, password="12345678")
        assert res.status_code == 400

    def test_cannot_self_assign_privileged_role(self, api_client):
        res = register(api_client, role=Role.MODERATOR)
        assert res.status_code == 400
        assert "role" in res.data["error"]["details"]

    def test_invalid_language_rejected(self, api_client):
        assert register(api_client, preferred_language="fr").status_code == 400


class TestLoginAndTokens:
    def test_login_success(self, api_client, user):
        res = api_client.post(LOGIN, {"email": user.email.upper(), "password": PASSWORD}, format="json")
        assert res.status_code == 200
        assert res.data["user"]["id"] == str(user.id)

    def test_login_wrong_password(self, api_client, user):
        res = api_client.post(LOGIN, {"email": user.email, "password": "wrong-password"}, format="json")
        assert res.status_code == 401
        assert res.data["error"]["code"] == "authentication_failed"

    def test_inactive_user_cannot_login(self, api_client, make_user):
        u = make_user(is_active=False)
        res = api_client.post(LOGIN, {"email": u.email, "password": PASSWORD}, format="json")
        assert res.status_code == 401

    def test_refresh_rotates_and_blacklists_old_token(self, api_client, user):
        tokens = api_client.post(LOGIN, {"email": user.email, "password": PASSWORD}, format="json").data["tokens"]
        res = api_client.post(REFRESH, {"refresh": tokens["refresh"]}, format="json")
        assert res.status_code == 200
        assert res.data["refresh"] != tokens["refresh"]
        reuse = api_client.post(REFRESH, {"refresh": tokens["refresh"]}, format="json")
        assert reuse.status_code == 401

    def test_logout_blacklists_refresh(self, api_client, user):
        tokens = api_client.post(LOGIN, {"email": user.email, "password": PASSWORD}, format="json").data["tokens"]
        api_client.credentials(HTTP_AUTHORIZATION=f"Bearer {tokens['access']}")
        assert api_client.post(LOGOUT, {"refresh": tokens["refresh"]}, format="json").status_code == 204
        assert BlacklistedToken.objects.count() == 1
        assert api_client.post(REFRESH, {"refresh": tokens["refresh"]}, format="json").status_code == 401

    def test_logout_rejects_someone_elses_token(self, auth_client, make_user, api_client):
        other = make_user()
        other_refresh = api_client.post(
            LOGIN, {"email": other.email, "password": PASSWORD}, format="json"
        ).data["tokens"]["refresh"]
        assert auth_client().post(LOGOUT, {"refresh": other_refresh}, format="json").status_code == 400

    def test_me_requires_auth(self, api_client):
        res = api_client.get(ME)
        assert res.status_code == 401
        assert res.data["error"]["code"] == "not_authenticated"

    def test_invalid_access_token(self, api_client):
        api_client.credentials(HTTP_AUTHORIZATION="Bearer not-a-token")
        res = api_client.get(ME)
        assert res.status_code == 401
        assert res.data["error"]["code"] == "token_not_valid"


class TestProfile:
    def test_me_get_and_patch(self, auth_client):
        client = auth_client()
        res = client.patch(ME, {"full_name": "New Name", "preferred_language": "de", "profile": {"bio": "Hello"}}, format="json")
        assert res.status_code == 200
        assert res.data["full_name"] == "New Name"
        assert res.data["profile"]["bio"] == "Hello"
        assert client.get(ME).data["preferred_language"] == "de"

    def test_me_cannot_change_role_or_email(self, auth_client):
        client = auth_client()
        client.patch(ME, {"role": "admin", "email": "x@example.test"}, format="json")
        client.user.refresh_from_db()
        assert client.user.role == Role.MEMBER
        assert client.user.email != "x@example.test"

    def test_avatar_upload_valid(self, auth_client, image_file):
        res = auth_client().post("/api/v1/auth/me/avatar/", {"avatar": image_file()}, format="multipart")
        assert res.status_code == 200
        assert res.data["profile"]["avatar"]

    def test_avatar_rejects_non_image(self, auth_client):
        from django.core.files.uploadedfile import SimpleUploadedFile

        bad = SimpleUploadedFile("evil.png", b"not an image", content_type="image/png")
        res = auth_client().post("/api/v1/auth/me/avatar/", {"avatar": bad}, format="multipart")
        assert res.status_code == 400

    def test_avatar_rejects_oversize(self, auth_client, image_file, settings):
        settings.MAX_IMAGE_UPLOAD_BYTES = 10
        res = auth_client().post("/api/v1/auth/me/avatar/", {"avatar": image_file()}, format="multipart")
        assert res.status_code == 400

    def test_avatar_remove(self, auth_client, image_file):
        client = auth_client()
        client.post("/api/v1/auth/me/avatar/", {"avatar": image_file()}, format="multipart")
        stored = client.user.profile.__class__.objects.get(pk=client.user.profile.pk).avatar
        assert stored and stored.storage.exists(stored.name)

        res = client.delete("/api/v1/auth/me/avatar/")
        assert res.status_code == 200
        assert res.data["profile"]["avatar"] is None
        assert not stored.storage.exists(stored.name)

    def test_avatar_remove_without_photo(self, auth_client):
        res = auth_client().delete("/api/v1/auth/me/avatar/")
        assert res.status_code == 200
        assert res.data["profile"]["avatar"] is None

    def test_avatar_remove_requires_auth(self, api_client):
        assert api_client.delete("/api/v1/auth/me/avatar/").status_code == 401


class TestPasswordReset:
    def test_reset_flow(self, api_client, user):
        res = api_client.post("/api/v1/auth/password-reset/", {"email": user.email}, format="json")
        assert res.status_code == 202
        assert len(mail.outbox) == 1
        import re

        uid, token = re.search(r"uid=([\w-]+)&token=([\w-]+)", mail.outbox[0].body).groups()
        res = api_client.post(
            "/api/v1/auth/password-reset/confirm/",
            {"uid": uid, "token": token, "new_password": "An0ther-strong-pass"}, format="json",
        )
        assert res.status_code == 204
        user.refresh_from_db()
        assert user.check_password("An0ther-strong-pass")

    def test_unknown_email_gives_same_response(self, api_client):
        res = api_client.post("/api/v1/auth/password-reset/", {"email": "nobody@example.test"}, format="json")
        assert res.status_code == 202
        assert len(mail.outbox) == 0

    def test_bad_token(self, api_client, user):
        from django.utils.encoding import force_bytes
        from django.utils.http import urlsafe_base64_encode

        res = api_client.post(
            "/api/v1/auth/password-reset/confirm/",
            {"uid": urlsafe_base64_encode(force_bytes(user.pk)), "token": "bad", "new_password": "An0ther-strong-pass"},
            format="json",
        )
        assert res.status_code == 400

    def test_change_password_revokes_refresh_tokens(self, api_client, user):
        tokens = api_client.post(LOGIN, {"email": user.email, "password": PASSWORD}, format="json").data["tokens"]
        api_client.credentials(HTTP_AUTHORIZATION=f"Bearer {tokens['access']}")
        res = api_client.post(
            "/api/v1/auth/change-password/", {"old_password": PASSWORD, "new_password": "An0ther-strong-pass"}, format="json"
        )
        assert res.status_code == 204
        api_client.credentials()
        assert api_client.post(REFRESH, {"refresh": tokens["refresh"]}, format="json").status_code == 401


def test_auth_endpoints_are_throttled(api_client, settings, user):
    from django.core.cache import cache

    cache.clear()
    settings.REST_FRAMEWORK = {**settings.REST_FRAMEWORK, "DEFAULT_THROTTLE_RATES": {**settings.REST_FRAMEWORK["DEFAULT_THROTTLE_RATES"], "auth": "3/min"}}
    from rest_framework.settings import api_settings
    from apps.common.throttles import AuthRateThrottle

    AuthRateThrottle.THROTTLE_RATES = api_settings.DEFAULT_THROTTLE_RATES
    try:
        codes = [api_client.post(LOGIN, {"email": user.email, "password": "x"}, format="json").status_code for _ in range(4)]
        assert codes[-1] == 429
    finally:
        cache.clear()
