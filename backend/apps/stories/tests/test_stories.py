import pytest

from apps.notifications.models import Notification
from apps.stories.models import Comment, Story, StoryStatus

pytestmark = pytest.mark.django_db
URL = "/api/v1/stories/"
BODY = "# My journey\n\nI still remember the first time I used a computer. " + "word " * 450


@pytest.fixture
def make_story(make_user):
    def _make(author=None, **kw):
        data = dict(title="From Kakuma to a new chapter", body=BODY, consent_to_publish=True, category="education")
        data.update(kw)
        return Story.objects.create(author=author or make_user(), **data)

    return _make


def test_model_computes_slug_reading_time(make_story):
    story = make_story()
    assert story.slug.startswith("from-kakuma-to-a-new-chapter-")
    assert story.reading_time == 3


def test_create_draft_with_cover(auth_client, image_file):
    client = auth_client()
    res = client.post(
        URL, {"title": "My story", "body": BODY, "category": "career", "language": "sw", "cover_image": image_file()},
        format="multipart",
    )
    assert res.status_code == 201, res.data
    assert res.data["status"] == "draft"
    assert res.data["excerpt"].startswith("My journey I still remember")
    assert res.data["cover_image"]
    assert res.data["author"]["id"] == str(client.user.id)
    assert "email" not in res.data["author"]


def test_cover_rejects_wrong_type(auth_client):
    from django.core.files.uploadedfile import SimpleUploadedFile

    gif = SimpleUploadedFile("a.gif", b"GIF89a", content_type="image/gif")
    res = auth_client().post(URL, {"title": "Title", "body": BODY, "cover_image": gif}, format="multipart")
    assert res.status_code == 400


def test_drafts_private_but_visible_to_author(api_client, auth_client, make_story):
    client = auth_client()
    draft = make_story(author=client.user, status=StoryStatus.DRAFT)
    assert api_client.get(URL).data["count"] == 0
    assert api_client.get(f"{URL}{draft.id}/").status_code == 404
    assert auth_client().get(f"{URL}{draft.id}/").status_code == 404
    assert client.get(f"{URL}{draft.id}/").status_code == 200
    assert client.get(URL, {"mine": "true"}).data["count"] == 1


def test_only_author_can_edit_or_delete(auth_client, make_story):
    story = make_story(status=StoryStatus.PUBLISHED)
    other = auth_client()
    assert other.patch(f"{URL}{story.id}/", {"title": "Hacked"}, format="json").status_code == 403
    assert other.delete(f"{URL}{story.id}/").status_code == 403
    owner = auth_client(story.author)
    assert owner.delete(f"{URL}{story.id}/").status_code == 204


def test_publish_requires_consent(auth_client, make_story):
    client = auth_client()
    story = make_story(author=client.user, consent_to_publish=False)
    res = client.post(f"{URL}{story.id}/publish/")
    assert res.status_code == 400
    assert "consent_to_publish" in res.data["error"]["details"]


def test_publish_goes_to_review_then_moderator_approves(auth_client, make_story, moderator):
    client = auth_client()
    story = make_story(author=client.user)
    res = client.post(f"{URL}{story.id}/publish/")
    assert res.status_code == 200 and res.data["status"] == "pending_review"
    assert client.post(f"{URL}{story.id}/publish/").status_code == 409

    mod = auth_client(moderator)
    queue = mod.get("/api/v1/moderation/stories/")
    assert [s["id"] for s in queue.data["results"]] == [str(story.id)]
    assert auth_client().get("/api/v1/moderation/stories/").status_code == 403

    res = mod.post(f"/api/v1/moderation/stories/{story.id}/approve/")
    assert res.status_code == 200 and res.data["status"] == "published"
    assert Notification.objects.filter(recipient=client.user, type="story_published").exists()
    assert mod.post(f"/api/v1/moderation/stories/{story.id}/approve/").status_code == 404  # left the queue


def test_moderator_rejects_with_reason(auth_client, make_story, moderator):
    story = make_story(status=StoryStatus.PENDING_REVIEW)
    mod = auth_client(moderator)
    assert mod.post(f"/api/v1/moderation/stories/{story.id}/reject/", {}, format="json").status_code == 400
    res = mod.post(f"/api/v1/moderation/stories/{story.id}/reject/", {"reason": "Please remove the phone number."}, format="json")
    assert res.data["status"] == "rejected"
    story.refresh_from_db()
    assert story.rejection_reason == "Please remove the phone number."
    n = Notification.objects.get(recipient=story.author)
    assert n.type == "story_rejected"


def test_moderator_publishes_own_story_directly(auth_client, make_story, moderator):
    story = make_story(author=moderator)
    assert auth_client(moderator).post(f"{URL}{story.id}/publish/").data["status"] == "published"


def test_review_disabled_publishes_directly(auth_client, make_story, settings):
    settings.STORIES_REQUIRE_REVIEW = False
    client = auth_client()
    story = make_story(author=client.user)
    assert client.post(f"{URL}{story.id}/publish/").data["status"] == "published"


def test_editing_published_story_returns_to_review(auth_client, make_story):
    client = auth_client()
    story = make_story(author=client.user, status=StoryStatus.PUBLISHED)
    res = client.patch(f"{URL}{story.id}/", {"body": BODY + " update"}, format="json")
    assert res.status_code == 200
    story.refresh_from_db()
    assert story.status == StoryStatus.PENDING_REVIEW


def test_like_and_bookmark(auth_client, make_story):
    story = make_story(status=StoryStatus.PUBLISHED)
    client = auth_client()
    res = client.post(f"{URL}{story.id}/like/")
    assert res.data == {"liked": True, "likes_count": 1}
    client.post(f"{URL}{story.id}/like/")  # idempotent
    assert client.get(f"{URL}{story.id}/").data["likes_count"] == 1
    assert client.delete(f"{URL}{story.id}/like/").data["likes_count"] == 0
    assert client.post(f"{URL}{story.id}/bookmark/").data == {"bookmarked": True}
    assert client.get(URL, {"bookmarked": "true"}).data["count"] == 1


def test_cannot_like_unpublished(auth_client, make_story):
    story = make_story()
    assert auth_client(story.author).post(f"{URL}{story.id}/like/").status_code == 404


def test_comments(api_client, auth_client, make_story, moderator):
    story = make_story(status=StoryStatus.PUBLISHED)
    commenter = auth_client()
    res = commenter.post(f"{URL}{story.id}/comments/", {"body": "Thank you for sharing!"}, format="json")
    assert res.status_code == 201
    assert Notification.objects.filter(recipient=story.author, type="story_comment").exists()
    assert commenter.post(f"{URL}{story.id}/comments/", {"body": "   "}, format="json").status_code == 400
    assert api_client.post(f"{URL}{story.id}/comments/", {"body": "anon"}, format="json").status_code == 401
    listing = api_client.get(f"{URL}{story.id}/comments/")
    assert listing.data["count"] == 1
    assert api_client.get(f"{URL}{story.id}/").data["comments_count"] == 1

    comment_id = res.data["id"]
    auth_client(moderator).post(f"/api/v1/moderation/comments/{comment_id}/hide/", {}, format="json")
    assert api_client.get(f"{URL}{story.id}/comments/").data["count"] == 0
    # Only the comment author (or a moderator) can delete.
    assert auth_client().delete(f"/api/v1/comments/{comment_id}/").status_code == 404
    assert commenter.delete(f"/api/v1/comments/{comment_id}/").status_code == 204
    assert not Comment.objects.exists()


def test_feed_ordering_by_likes(api_client, auth_client, make_story):
    from django.utils import timezone

    a = make_story(title="A", status=StoryStatus.PUBLISHED, published_at=timezone.now())
    b = make_story(title="B", status=StoryStatus.PUBLISHED, published_at=timezone.now())
    auth_client().post(f"{URL}{b.id}/like/")
    titles = [s["title"] for s in api_client.get(URL, {"ordering": "-likes_count"}).data["results"]]
    assert titles == ["B", "A"]
    assert a  # noqa


@pytest.mark.parametrize(
    "raw, expected",
    [
        ("# **Captain Daniel Lokiru: From Kakuma to the Open Seas**", "Captain Daniel Lokiru: From Kakuma to the Open Seas"),
        ("## A __new__ beginning", "A new beginning"),
        ("*Hope in Kakuma*", "Hope in Kakuma"),
        ("`Code` and ~~old~~ words", "Code and old words"),
        ("C# basics for beginners", "C# basics for beginners"),
        ("#1 tip: why 2*3 matters", "#1 tip: why 2*3 matters"),
        ("  Plain   title  ", "Plain title"),
    ],
)
def test_clean_title(raw, expected):
    from apps.stories.text import clean_title

    assert clean_title(raw) == expected


def test_markdown_is_removed_from_titles_on_save(auth_client):
    res = auth_client().post(URL, {"title": "# **Hope in Kakuma**", "body": BODY}, format="json")
    assert res.status_code == 201, res.data
    assert res.data["title"] == "Hope in Kakuma"


def test_title_that_is_only_markdown_is_too_short(auth_client):
    res = auth_client().post(URL, {"title": "# ** **", "body": BODY}, format="json")
    assert res.status_code == 400
    assert "title" in res.data["error"]["details"]
