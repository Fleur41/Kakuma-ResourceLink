import pytest

from apps.contributions.models import CommunityProject, ProjectStatus
from apps.moderation.models import ModerationDecision, Report
from apps.stories.models import Story, StoryStatus

pytestmark = pytest.mark.django_db


def test_report_and_resolve(auth_client, make_user, moderator):
    story = Story.objects.create(author=make_user(), title="T", body="b", status=StoryStatus.PUBLISHED)
    reporter = auth_client()
    payload = {"target_type": "story", "target_id": str(story.id), "reason": "privacy", "details": "Shows a phone number"}
    res = reporter.post("/api/v1/moderation/reports/", payload, format="json")
    assert res.status_code == 201
    reporter.post("/api/v1/moderation/reports/", payload, format="json")  # no duplicate open report
    assert Report.objects.count() == 1
    assert reporter.get("/api/v1/moderation/reports/").status_code == 403

    mod = auth_client(moderator)
    assert mod.get("/api/v1/moderation/reports/").data["count"] == 1
    res = mod.post(f"/api/v1/moderation/reports/{res.data['id']}/decide/", {"resolved": True}, format="json")
    assert res.data["status"] == "resolved"
    assert ModerationDecision.objects.filter(action="resolve_report").exists()


def test_report_unknown_target(auth_client):
    import uuid

    res = auth_client().post(
        "/api/v1/moderation/reports/", {"target_type": "story", "target_id": str(uuid.uuid4()), "reason": "spam"}, format="json"
    )
    assert res.status_code == 400


def test_project_approval(auth_client, make_user, moderator):
    owner = make_user()
    project = CommunityProject.objects.create(owner=owner, title="Library", summary="s", description="d")
    mod = auth_client(moderator)
    assert mod.get("/api/v1/moderation/projects/").data["count"] == 1
    res = mod.post(f"/api/v1/moderation/projects/{project.id}/approve/", {}, format="json")
    assert res.data["status"] == ProjectStatus.ACTIVE
    assert owner.notifications.filter(type="project_status").exists()
