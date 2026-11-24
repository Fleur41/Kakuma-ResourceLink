"""AI assistant tests. Claude is always mocked — no network calls, no real API key."""
from types import SimpleNamespace
from unittest import mock

import anthropic
import httpx2 as httpx
import pytest

from apps.ai_assistant import grounding
from apps.ai_assistant.claude_client import ClaudeClient, Completion
from apps.ai_assistant.exceptions import AIUnavailable
from apps.ai_assistant.models import AIConversation, AIMessage
from apps.opportunities.models import Opportunity
from apps.stories.models import Story

pytestmark = pytest.mark.django_db
CHAT = "/api/v1/ai/chat/"


@pytest.fixture
def fake_complete():
    with mock.patch.object(ClaudeClient, "__init__", return_value=None), mock.patch.object(
        ClaudeClient, "complete", return_value=Completion("Here is what I found.", "claude-opus-5-5", 120, 40)
    ) as complete:
        yield complete


@pytest.fixture
def scholarship(db):
    return Opportunity.objects.create(
        title="Sample undergraduate scholarship", category="scholarship", organization="Sample partner",
        summary="Tuition support", description="Illustrative", requirements=["Certificate"], is_verified=True,
    )


def test_chat_grounds_on_platform_data_and_persists(auth_client, fake_complete, scholarship):
    client = auth_client()
    res = client.post(CHAT, {"message": "How can I find scholarships?", "language": "sw"}, format="json")
    assert res.status_code == 200, res.data
    assert res.data["message"]["content"] == "Here is what I found."
    assert res.data["sources"] == [{"type": "opportunity", "id": str(scholarship.id), "title": scholarship.title}]

    kwargs = fake_complete.call_args.kwargs
    turn = kwargs["messages"][-1]["content"]
    assert "<platform_content>" in turn and scholarship.title in turn and "verified by platform staff" in turn
    assert "Reply in Kiswahili" in turn
    assert "never invent" in kwargs["system"].lower()

    conv = AIConversation.objects.get(user=client.user)
    assert conv.language == "sw"
    assert list(conv.messages.values_list("role", flat=True)) == ["user", "assistant"]
    assert conv.messages.get(role="user").content == "How can I find scholarships?"  # raw text stored, not the prompt


def test_follow_up_includes_history(auth_client, fake_complete):
    client = auth_client()
    cid = client.post(CHAT, {"message": "Hello there"}, format="json").data["conversation_id"]
    client.post(CHAT, {"message": "Tell me more", "conversation_id": cid}, format="json")
    messages = fake_complete.call_args.kwargs["messages"]
    assert [m["role"] for m in messages] == ["user", "assistant", "user"]
    assert messages[0]["content"] == "Hello there"
    assert AIMessage.objects.filter(conversation_id=cid).count() == 4


def test_no_results_tells_model_nothing_found(auth_client, fake_complete):
    auth_client().post(CHAT, {"message": "Quantum basket weaving grants"}, format="json")
    assert "No matching platform records were found." in fake_complete.call_args.kwargs["messages"][-1]["content"]


def test_cannot_use_other_users_conversation(auth_client, fake_complete, make_user):
    conv = AIConversation.objects.create(user=make_user())
    res = auth_client().post(CHAT, {"message": "hi", "conversation_id": str(conv.id)}, format="json")
    assert res.status_code == 404


def test_explain_opportunity_and_summarize_story(auth_client, fake_complete, scholarship, make_user):
    client = auth_client()
    res = client.post(CHAT, {"message": "Explain", "task": "explain_opportunity", "target_id": str(scholarship.id)}, format="json")
    assert res.data["sources"][0]["id"] == str(scholarship.id)
    draft = Story.objects.create(author=make_user(), title="Draft", body="secret draft")
    res = client.post(CHAT, {"message": "Summarise", "task": "summarize_story", "target_id": str(draft.id)}, format="json")
    assert res.status_code == 404  # unpublished stories are never sent to the model
    assert client.post(CHAT, {"message": "x", "task": "explain_opportunity"}, format="json").status_code == 400


def test_translate_requires_target(auth_client, fake_complete):
    client = auth_client()
    assert client.post(CHAT, {"message": "Good morning", "task": "translate"}, format="json").status_code == 400
    res = client.post(CHAT, {"message": "Good morning", "task": "translate", "target_language": "it"}, format="json")
    assert res.status_code == 200
    assert "Target language: Italian" in fake_complete.call_args.kwargs["messages"][-1]["content"]


def test_input_validation(auth_client, fake_complete, settings):
    client = auth_client()
    assert client.post(CHAT, {"message": ""}, format="json").status_code == 400
    assert client.post(CHAT, {"message": "x" * (settings.AI_MAX_INPUT_CHARS + 1)}, format="json").status_code == 400
    fake_complete.assert_not_called()


def test_requires_auth(api_client):
    assert api_client.post(CHAT, {"message": "hi"}, format="json").status_code == 401


def test_missing_api_key_returns_503(auth_client, settings):
    settings.ANTHROPIC_API_KEY = ""
    res = auth_client().post(CHAT, {"message": "hi"}, format="json")
    assert res.status_code == 503 and res.data["error"]["code"] == "ai_unavailable"
    assert not AIMessage.objects.exists()  # nothing fabricated or stored


def _response(stop_reason="end_turn", text="Answer"):
    return SimpleNamespace(
        stop_reason=stop_reason, model="claude-opus-5-5",
        content=[SimpleNamespace(type="thinking", thinking=""), SimpleNamespace(type="text", text=text)],
        usage=SimpleNamespace(input_tokens=10, output_tokens=5),
    )


def test_client_sends_expected_request(settings):
    settings.AI_ENABLE_FALLBACKS = True
    client = ClaudeClient(api_key="test")
    with mock.patch.object(client._client.beta.messages, "create", return_value=_response()) as create:
        result = client.complete(system="SYS", messages=[{"role": "user", "content": "hi"}])
    assert result.text == "Answer" and result.input_tokens == 10
    kw = create.call_args.kwargs
    assert kw["model"] == settings.AI_MODEL
    assert kw["fallbacks"] == "default" and kw["betas"] == ["server-side-fallback-2026-07-01"]
    assert kw["system"][0]["cache_control"] == {"type": "ephemeral"}
    assert "thinking" not in kw and "temperature" not in kw


def test_client_refusal_is_error(settings):
    settings.AI_ENABLE_FALLBACKS = False
    client = ClaudeClient(api_key="test")
    with mock.patch.object(client._client.messages, "create", return_value=_response("refusal")):
        with pytest.raises(Exception) as exc:
            client.complete(system="S", messages=[{"role": "user", "content": "x"}])
    assert exc.value.default_code == "ai_error"


def test_client_maps_api_errors(settings):
    settings.AI_ENABLE_FALLBACKS = False
    client = ClaudeClient(api_key="test")
    req = httpx.Request("POST", "https://api.anthropic.com/v1/messages")
    err = anthropic.RateLimitError("slow down", response=httpx.Response(429, request=req), body=None)
    with mock.patch.object(client._client.messages, "create", side_effect=err):
        with pytest.raises(AIUnavailable):
            client.complete(system="S", messages=[{"role": "user", "content": "x"}])
    with mock.patch.object(client._client.messages, "create", side_effect=anthropic.APIConnectionError(request=req)):
        with pytest.raises(AIUnavailable):
            client.complete(system="S", messages=[{"role": "user", "content": "x"}])


def test_keywords_skip_stopwords():
    assert grounding.keywords("Where can I find scholarships in Kakuma?") == ["scholarships"]


def test_conversation_endpoints(auth_client, fake_complete, make_user):
    client = auth_client()
    cid = client.post(CHAT, {"message": "Hello"}, format="json").data["conversation_id"]
    AIConversation.objects.create(user=make_user())
    assert client.get("/api/v1/ai/conversations/").data["count"] == 1
    detail = client.get(f"/api/v1/ai/conversations/{cid}/").data
    assert len(detail["messages"]) == 2
    assert client.delete(f"/api/v1/ai/conversations/{cid}/").status_code == 204
