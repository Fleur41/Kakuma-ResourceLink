"""Shared pytest fixtures. All data is synthetic — no real people or credentials."""
import io
import itertools

import pytest
from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image
from rest_framework.test import APIClient

from apps.accounts.models import Role, User
from apps.accounts.services import issue_tokens

_counter = itertools.count(1)
PASSWORD = "Str0ng-test-passphrase"


@pytest.fixture
def api_client() -> APIClient:
    return APIClient()


@pytest.fixture
def make_user(db):
    def _make(role: str = Role.MEMBER, **extra) -> User:
        n = next(_counter)
        defaults = {"email": f"user{n}@example.test", "full_name": f"Test User {n}", "role": role}
        defaults.update(extra)
        return User.objects.create_user(password=PASSWORD, **defaults)

    return _make


@pytest.fixture
def user(make_user):
    return make_user()


@pytest.fixture
def moderator(make_user):
    return make_user(role=Role.MODERATOR)


@pytest.fixture
def auth_client(make_user):
    """Return a factory producing an APIClient authenticated as the given (or a new) user."""

    def _client(u: User | None = None) -> APIClient:
        u = u or make_user()
        client = APIClient()
        client.credentials(HTTP_AUTHORIZATION=f"Bearer {issue_tokens(u)['access']}")
        client.user = u
        return client

    return _client


@pytest.fixture
def image_file():
    def _make(name: str = "cover.png", fmt: str = "PNG", content_type: str = "image/png") -> SimpleUploadedFile:
        buf = io.BytesIO()
        Image.new("RGB", (20, 20), (7, 91, 75)).save(buf, format=fmt)
        return SimpleUploadedFile(name, buf.getvalue(), content_type=content_type)

    return _make


@pytest.fixture(autouse=True)
def _media_root(settings, tmp_path):
    settings.MEDIA_ROOT = tmp_path / "media"
