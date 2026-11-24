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

    n = notify(make_user(preferred_language="de"), "story_published", _("Your story is live"),
               _("“%(title)s” has been published."), params={"title": "Mein Weg"})
    assert (n.title, n.body) == ("Deine Geschichte ist online", "„Mein Weg“ wurde veröffentlicht.")
    n = notify(make_user(preferred_language="sw"), "story_published", _("Your story is live"))
    assert n.title == "Hadithi yako imechapishwa"
    n = notify(make_user(preferred_language="en"), "story_published", _("Your story is live"))
    assert n.title == "Your story is live"


def test_translated_status_label_inside_message(make_user):
    from django.utils.translation import gettext_lazy as _

    from apps.mentorship.models import MentorshipStatus

    n = notify(make_user(preferred_language="es"), "mentorship_update", _("Mentorship request update"),
               _("Your mentorship request “%(goal)s” is now %(status)s."),
               params={"goal": "Becas", "status": MentorshipStatus.ACCEPTED.label})
    assert n.body == "Tu solicitud de mentoría «Becas» ahora está: Aceptada."


def test_api_errors_follow_accept_language(api_client):
    res = api_client.post("/api/v1/auth/login/", {"email": "nobody@example.org", "password": "wrong-pass-1"},
                          format="json", HTTP_ACCEPT_LANGUAGE="it")
    assert res.status_code in (400, 401)
    assert "Email o password non corretti." in str(res.data)


def test_inactive_recipient_skipped(make_user):
    assert notify(make_user(is_active=False), "system", "x") is None
