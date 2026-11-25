"""Assistant orchestration: conversation persistence, context management and grounding."""
from django.conf import settings
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils.translation import gettext_lazy as _
from rest_framework.exceptions import ValidationError

from apps.opportunities.models import Opportunity, OpportunityStatus
from apps.stories.models import Story, StoryStatus

from . import grounding
from .llm import LLMClient, get_client
from .models import AIConversation, AIMessage, MessageRole
from .prompts import LANGUAGE_NAMES, SYSTEM_PROMPT, TASK_INSTRUCTIONS


def _history(conversation: AIConversation | None) -> list[dict]:
    """Last N stored messages, oldest first, starting with a user turn (API requirement)."""
    if conversation is None:
        return []
    recent = list(conversation.messages.order_by("-created_at")[: settings.AI_HISTORY_MESSAGES])[::-1]
    while recent and recent[0].role != MessageRole.USER:
        recent.pop(0)
    return [{"role": m.role, "content": m.content} for m in recent]


def _task_sources(task: str, target_id, message: str) -> list[grounding.Source]:
    if task == "explain_opportunity":
        if not target_id:
            raise ValidationError({"target_id": [_("Choose an opportunity to explain.")]})
        opp = get_object_or_404(Opportunity, pk=target_id, status=OpportunityStatus.PUBLISHED)
        return [grounding.opportunity_source(opp)]
    if task == "summarize_story":
        if not target_id:
            raise ValidationError({"target_id": [_("Choose a story to summarise.")]})
        story = get_object_or_404(Story.objects.select_related("author"), pk=target_id, status=StoryStatus.PUBLISHED)
        return [grounding.story_source(story, full=True)]
    if task == "translate":
        return []
    return grounding.retrieve(message)


def _user_turn(task: str, message: str, sources, language: str, target_language: str | None) -> str:
    lang_name = LANGUAGE_NAMES.get(target_language or language, "English")
    if task == "translate":
        return (
            f"{TASK_INSTRUCTIONS['translate']}\nTarget language: {lang_name}\n"
            f"<text_to_translate>\n{grounding.neutralize(message)}\n</text_to_translate>"
        )
    return (
        f"{grounding.render(sources)}\n\nTask: {TASK_INSTRUCTIONS[task]}\n"
        f"Reply in {lang_name}.\n<user_message>\n{message}\n</user_message>"
    )


def chat(
    *, user, message: str, conversation_id=None, language: str | None = None, task: str = "chat",
    target_id=None, target_language: str | None = None, client: LLMClient | None = None,
) -> tuple[AIConversation, AIMessage]:
    conversation = (
        get_object_or_404(AIConversation, pk=conversation_id, user=user) if conversation_id else None
    )
    language = language or (conversation.language if conversation else user.preferred_language) or "en"
    sources = _task_sources(task, target_id, message)

    messages = _history(conversation) + [
        {"role": "user", "content": _user_turn(task, message, sources, language, target_language)}
    ]
    client = client or get_client()
    # Network call happens outside any DB transaction.
    completion = client.complete(system=SYSTEM_PROMPT, messages=messages)

    with transaction.atomic():
        if conversation is None:
            conversation = AIConversation.objects.create(user=user, language=language, title=message[:80].strip())
        AIMessage.objects.create(conversation=conversation, role=MessageRole.USER, content=message)
        reply = AIMessage.objects.create(
            conversation=conversation, role=MessageRole.ASSISTANT, content=completion.text,
            sources=[s.ref() for s in sources], model=completion.model,
            input_tokens=completion.input_tokens, output_tokens=completion.output_tokens,
        )
        conversation.save(update_fields=["updated_at"])
    return conversation, reply
