import pytest
from django.db import IntegrityError
from django.utils import timezone

from apps.diaspora.models import AlumniProfile
from apps.mentorship.models import MentorshipRequest
from apps.notifications.models import Notification

pytestmark = pytest.mark.django_db
URL = "/api/v1/mentorship/requests/"


@pytest.fixture
def mentor_profile(make_user):
    return AlumniProfile.objects.create(
        user=make_user(role="mentor"), country="Germany", profession="Engineer", is_mentor=True,
        visibility="members", consent_given_at=timezone.now(),
    )


def payload(profile, **kw):
    data = {"mentor_id": str(profile.id), "goal": "Prepare for a software career", "help_areas": ["career", "technical"],
            "message": "Hello, I would love guidance on getting started."}
    data.update(kw)
    return data


def test_full_lifecycle(auth_client, mentor_profile):
    mentee = auth_client()
    res = mentee.post(URL, payload(mentor_profile), format="json")
    assert res.status_code == 201 and res.data["status"] == "pending"
    rid = res.data["id"]
    assert Notification.objects.filter(recipient=mentor_profile.user, type="mentorship_request").exists()

    mentor = auth_client(mentor_profile.user)
    assert mentor.get(URL, {"box": "received"}).data["count"] == 1
    assert mentee.get(URL, {"box": "received"}).data["count"] == 0
    # Mentee cannot accept their own request.
    assert mentee.patch(f"{URL}{rid}/", {"status": "accepted"}, format="json").status_code == 409
    res = mentor.patch(f"{URL}{rid}/", {"status": "accepted", "response_message": "Happy to help!"}, format="json")
    assert res.status_code == 200 and res.data["status"] == "accepted" and res.data["responded_at"]
    assert Notification.objects.filter(recipient=mentee.user, type="mentorship_update").exists()
    assert mentor.patch(f"{URL}{rid}/", {"status": "declined"}, format="json").data["error"]["code"] == "invalid_transition"
    assert mentee.patch(f"{URL}{rid}/", {"status": "completed"}, format="json").data["status"] == "completed"


def test_mentee_can_cancel_pending(auth_client, mentor_profile):
    mentee = auth_client()
    rid = mentee.post(URL, payload(mentor_profile), format="json").data["id"]
    assert mentee.patch(f"{URL}{rid}/", {"status": "cancelled"}, format="json").data["status"] == "cancelled"
    # After cancelling, a new request is allowed.
    assert mentee.post(URL, payload(mentor_profile), format="json").status_code == 201


def test_duplicate_pending_conflict(auth_client, mentor_profile):
    mentee = auth_client()
    mentee.post(URL, payload(mentor_profile), format="json")
    res = mentee.post(URL, payload(mentor_profile), format="json")
    assert res.status_code == 409 and res.data["error"]["code"] == "conflict"


def test_validation(auth_client, mentor_profile):
    client = auth_client()
    res = client.post(URL, payload(mentor_profile, help_areas=[], message="short"), format="json")
    assert res.status_code == 400
    assert {"help_areas", "message"} <= set(res.data["error"]["details"])
    assert client.post(URL, payload(mentor_profile, help_areas=["astrology"]), format="json").status_code == 400


def test_cannot_request_non_mentor_or_private_or_self(auth_client, mentor_profile):
    mentor_profile.is_mentor = False
    mentor_profile.save()
    assert auth_client().post(URL, payload(mentor_profile), format="json").status_code == 400
    mentor_profile.is_mentor, mentor_profile.visibility = True, "private"
    mentor_profile.save()
    assert auth_client().post(URL, payload(mentor_profile), format="json").status_code == 404
    mentor_profile.visibility = "members"
    mentor_profile.save()
    assert auth_client(mentor_profile.user).post(URL, payload(mentor_profile), format="json").status_code == 400


def test_outsider_cannot_see_request(auth_client, mentor_profile):
    rid = auth_client().post(URL, payload(mentor_profile), format="json").data["id"]
    outsider = auth_client()
    assert outsider.get(f"{URL}{rid}/").status_code == 404
    assert outsider.patch(f"{URL}{rid}/", {"status": "cancelled"}, format="json").status_code == 404


def test_db_constraints(make_user):
    a, b = make_user(), make_user()
    MentorshipRequest.objects.create(mentee=a, mentor=b, goal="g", help_areas=["career"], message="m")
    with pytest.raises(IntegrityError):
        MentorshipRequest.objects.create(mentee=a, mentor=b, goal="g", help_areas=["career"], message="m")


def test_db_rejects_self_request(make_user):
    a = make_user()
    with pytest.raises(IntegrityError):
        MentorshipRequest.objects.create(mentee=a, mentor=a, goal="g", help_areas=[], message="m")
