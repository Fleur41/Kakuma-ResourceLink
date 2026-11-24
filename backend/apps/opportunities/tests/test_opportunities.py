from datetime import timedelta

import pytest
from django.db import IntegrityError
from django.utils import timezone

from apps.opportunities.models import Opportunity, OpportunityBookmark, OpportunityStatus

pytestmark = pytest.mark.django_db
URL = "/api/v1/opportunities/"


@pytest.fixture
def make_opportunity(db):
    def _make(**kw):
        data = dict(
            title="Sample undergraduate scholarship", category="scholarship", organization="Sample education partner",
            summary="Tuition support (sample listing)", description="Illustrative sample content.",
            requirements=["Secondary school certificate"],
        )
        data.update(kw)
        return Opportunity.objects.create(**data)

    return _make


def test_list_is_public_and_paginated(api_client, make_opportunity):
    for i in range(3):
        make_opportunity(title=f"Opp {i}")
    res = api_client.get(URL, {"page_size": 2})
    assert res.status_code == 200
    assert res.data["count"] == 3 and len(res.data["results"]) == 2 and res.data["next"]
    assert res.data["results"][0]["is_bookmarked"] is False


def test_drafts_hidden_from_public(api_client, make_opportunity):
    draft = make_opportunity(status=OpportunityStatus.DRAFT)
    assert api_client.get(URL).data["count"] == 0
    assert api_client.get(f"{URL}{draft.id}/").status_code == 404


def test_filters_and_search(api_client, make_opportunity):
    today = timezone.localdate()
    make_opportunity(title="Welding course", category="vocational", deadline=today + timedelta(days=5))
    make_opportunity(title="Old job", category="job", deadline=today - timedelta(days=1))
    make_opportunity(title="Remote internship", category="internship", is_remote=True)
    assert api_client.get(URL, {"category": "vocational"}).data["count"] == 1
    assert api_client.get(URL, {"open": "true"}).data["count"] == 2
    assert api_client.get(URL, {"search": "welding"}).data["count"] == 1
    assert api_client.get(URL, {"is_remote": "true"}).data["results"][0]["title"] == "Remote internship"
    ordered = api_client.get(URL, {"ordering": "deadline"}).data["results"]
    assert ordered[0]["title"] == "Old job"


def test_bookmark_flow(auth_client, make_opportunity):
    client = auth_client()
    opp = make_opportunity()
    assert client.post(f"{URL}{opp.id}/bookmark/").status_code == 201
    assert client.post(f"{URL}{opp.id}/bookmark/").status_code == 200  # idempotent
    assert client.get(f"{URL}{opp.id}/").data["is_bookmarked"] is True
    assert client.get(URL, {"bookmarked": "true"}).data["count"] == 1
    assert client.delete(f"{URL}{opp.id}/bookmark/").status_code == 204
    assert OpportunityBookmark.objects.count() == 0


def test_bookmark_requires_auth(api_client, make_opportunity):
    assert api_client.post(f"{URL}{make_opportunity().id}/bookmark/").status_code == 401


def test_bookmark_unique_constraint(user, make_opportunity):
    opp = make_opportunity()
    OpportunityBookmark.objects.create(user=user, opportunity=opp)
    with pytest.raises(IntegrityError):
        OpportunityBookmark.objects.create(user=user, opportunity=opp)


def test_only_moderators_can_write(auth_client, moderator):
    payload = {"title": "New", "category": "job", "organization": "Org", "summary": "S", "description": "D"}
    assert auth_client().post(URL, payload, format="json").status_code == 403
    res = auth_client(moderator).post(URL, payload, format="json")
    assert res.status_code == 201
    assert res.data["is_verified"] is False


def test_validation_errors(auth_client, moderator):
    res = auth_client(moderator).post(URL, {"title": "", "category": "nope"}, format="json")
    assert res.status_code == 400
    assert {"title", "category"} <= set(res.data["error"]["details"])
