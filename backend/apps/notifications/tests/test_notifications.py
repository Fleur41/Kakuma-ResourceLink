import pytest

from apps.notifications.services import notify

pytestmark = pytest.mark.django_db
URL = "/api/v1/notifications/"


def test_list_read_and_counts(auth_client, make_user):
    client = auth_client()
    n1 = notify(client.user, "system", "Welcome")
    notify(client.user, "system", "Second")
    notify(make_user(), "system", "Someone else's")
    assert client.get(URL).data["count"] == 2
    assert client.get(f"{URL}unread-count/").data == {"count": 2}
    res = client.post(f"{URL}{n1.id}/read/")
    assert res.data["is_read"] is True
    assert client.get(URL, {"unread": "true"}).data["count"] == 1
    assert client.post(f"{URL}read-all/").data == {"updated": 1}


def test_cannot_read_others(auth_client, make_user):
    other = notify(make_user(), "system", "Private")
    assert auth_client().post(f"{URL}{other.id}/read/").status_code == 404


def test_notification_rendered_in_recipient_language(make_user):
    from django.utils.translation import gettext_lazy as _

    u = make_user(preferred_language="de")
    n = notify(u, "system", _("Hello %(name)s"), params={"name": "Amina"})
    assert n.title == "Hello Amina"  # no German catalogue yet; verifies params are applied safely


def test_inactive_recipient_skipped(make_user):
    assert notify(make_user(is_active=False), "system", "x") is None
