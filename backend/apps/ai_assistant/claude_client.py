"""Thin wrapper around the Anthropic SDK. The only module that talks to the Claude API."""
import logging

import anthropic
from django.conf import settings
from django.utils.translation import gettext_lazy as _

from .exceptions import AIError, AIUnavailable
from .llm import Completion

logger = logging.getLogger(__name__)

FALLBACK_BETA = "server-side-fallback-2026-07-01"


class ClaudeClient:
    def __init__(self, api_key: str | None = None):
        key = api_key if api_key is not None else settings.ANTHROPIC_API_KEY
        if not key:
            raise AIUnavailable()
        self._client = anthropic.Anthropic(api_key=key, timeout=settings.AI_TIMEOUT_SECONDS, max_retries=2)

    def complete(self, *, system: str, messages: list[dict]) -> Completion:
        params = dict(
            model=settings.AI_MODEL,
            max_tokens=settings.AI_MAX_OUTPUT_TOKENS,
            # Stable system prompt first so the prefix can be cached across turns and users.
            system=[{"type": "text", "text": system, "cache_control": {"type": "ephemeral"}}],
            messages=messages,
            output_config={"effort": settings.AI_EFFORT},
        )
        try:
            if settings.AI_ENABLE_FALLBACKS:
                # On a safety-classifier refusal the API re-runs the request on a fallback model.
                response = self._client.beta.messages.create(betas=[FALLBACK_BETA], fallbacks="default", **params)
            else:
                response = self._client.messages.create(**params)
        except anthropic.AuthenticationError:
            logger.error("Claude API authentication failed — check ANTHROPIC_API_KEY")
            raise AIUnavailable()
        except anthropic.RateLimitError:
            logger.warning("Claude API rate limited")
            raise AIUnavailable(_("The assistant is busy right now. Please try again in a minute."))
        except anthropic.BadRequestError as exc:
            logger.error("Claude API rejected request: %s", exc.message)
            raise AIError()
        except anthropic.APIStatusError as exc:
            logger.error("Claude API error %s (request %s)", exc.status_code, getattr(exc, "request_id", None))
            raise AIUnavailable() if exc.status_code >= 500 else AIError()
        except anthropic.APIConnectionError:
            logger.warning("Could not reach Claude API")
            raise AIUnavailable()

        if response.stop_reason == "refusal":
            raise AIError(_("The assistant can't help with this request."))
        text = "".join(block.text for block in response.content if block.type == "text").strip()
        if not text:
            raise AIError()
        return Completion(
            text=text,
            model=response.model,
            input_tokens=response.usage.input_tokens or 0,
            output_tokens=response.usage.output_tokens or 0,
        )
