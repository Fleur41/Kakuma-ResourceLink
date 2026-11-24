from decimal import Decimal

import pytest
from django.db import IntegrityError

from apps.contributions.models import CommunityProject, Contribution, ProjectStatus

pytestmark = pytest.mark.django_db
URL = "/api/v1/projects/"


@pytest.fixture
def active_project(make_user):
    return CommunityProject.objects.create(
        owner=make_user(), title="Solar study lamps", summary="Lamps for students", description="Sample project",
        status=ProjectStatus.ACTIVE, funding_goal=Decimal("500"), currency="USD",
        accepted_contribution_types=["mentoring", "equipment", "financial_pledge"],
    )


def test_create_goes_to_review_and_is_hidden(api_client, auth_client):
    client = auth_client()
    res = client.post(URL, {"title": "Community library", "summary": "Books", "description": "Sample", "currency": "kes"}, format="json")
    assert res.status_code == 201 and res.data["status"] == "pending_review" and res.data["currency"] == "KES"
    assert api_client.get(URL).data["count"] == 0
    assert client.get(f"{URL}{res.data['id']}/").status_code == 200
    assert client.get(URL, {"mine": "true"}).data["count"] == 1


def test_pledges_and_summary(api_client, auth_client, active_project):
    supporter = auth_client()
    res = supporter.post(f"{URL}{active_project.id}/contributions/", {"type": "financial_pledge", "description": "Monthly support", "amount": "25.50"}, format="json")
    assert res.status_code == 201
    assert res.data["status"] == "pledged" and res.data["currency"] == "USD"
    supporter.post(f"{URL}{active_project.id}/contributions/", {"type": "mentoring", "description": "Weekly coding sessions"}, format="json")
    summary = api_client.get(f"{URL}{active_project.id}/").data["contribution_summary"]
    assert summary["total_contributions"] == 2
    assert summary["pledged_amount"] == "25.50"
    assert "not completed payments" in summary["note"]
    assert active_project.owner.notifications.filter(type="project_contribution").count() == 2


def test_contribution_validation(auth_client, active_project):
    client = auth_client()
    url = f"{URL}{active_project.id}/contributions/"
    assert client.post(url, {"type": "financial_pledge", "description": "No amount"}, format="json").status_code == 400
    assert client.post(url, {"type": "volunteering", "description": "Not accepted type"}, format="json").status_code == 400
    # Non-financial contributions never store an amount.
    res = client.post(url, {"type": "equipment", "description": "Two laptops", "amount": "999"}, format="json")
    assert res.status_code == 201 and res.data["amount"] is None


def test_cannot_contribute_to_inactive_project(auth_client, make_user):
    project = CommunityProject.objects.create(owner=make_user(), title="P", summary="s", description="d")
    res = auth_client(project.owner).post(f"{URL}{project.id}/contributions/", {"type": "mentoring", "description": "help"}, format="json")
    assert res.status_code == 409


def test_contribution_list_privacy(auth_client, active_project):
    a, b = auth_client(), auth_client()
    url = f"{URL}{active_project.id}/contributions/"
    a.post(url, {"type": "mentoring", "description": "A helps"}, format="json")
    b.post(url, {"type": "mentoring", "description": "B helps"}, format="json")
    assert a.get(url).data["count"] == 1
    assert auth_client(active_project.owner).get(url).data["count"] == 2


def test_owner_only_edit(auth_client, active_project):
    assert auth_client().patch(f"{URL}{active_project.id}/", {"title": "x"}, format="json").status_code == 403
    res = auth_client(active_project.owner).patch(f"{URL}{active_project.id}/", {"title": "Solar lamps", "status": "completed"}, format="json")
    assert res.data["title"] == "Solar lamps" and res.data["status"] == "active"  # status is moderator-controlled


def test_db_check_constraint(active_project, user):
    with pytest.raises(IntegrityError):
        Contribution.objects.create(project=active_project, contributor=user, type="financial_pledge", description="x", amount=None)
