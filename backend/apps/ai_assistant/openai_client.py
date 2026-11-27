"""Thin wrapper around the OpenAI SDK (Responses API). The only module that talks to the OpenAI API."""
import logging

import openai
from django.conf import settings
from django.utils.translation import gettext_lazy as _

from .exceptions import AIError, AIUnavailable
from .llm import Completion

logger = logging.getLogger(__name__)


class OpenAIClient:
    def __init__(self, api_key: str | None = None):
        key = api_key if api_key is not None else settings.OPENAI_API_KEY
        if not key:
            raise AIUnavailable()
        self._client = openai.OpenAI(api_key=key, timeout=settings.AI_TIMEOUT_SECONDS, max_retries=2)

    def complete(self, *, system: str, messages: list[dict]) -> Completion:
        try:
            response = self._client.responses.create(
                model=settings.AI_MODEL,
                # The system prompt goes first and never changes, so OpenAI can reuse its cached prefix.
                instructions=system,
                input=messages,
                max_output_tokens=settings.AI_MAX_OUTPUT_TOKENS,
                reasoning={"effort": settings.AI_EFFORT},
                # Conversations are already saved in our own database; don't keep a copy at OpenAI.
                store=False,
            )
        except openai.AuthenticationError:
            logger.error("OpenAI API authentication failed — check OPENAI_API_KEY")
            raise AIUnavailable()
        except openai.RateLimitError:
            logger.warning("OpenAI API rate limited")
            raise AIUnavailable(_("The assistant is busy right now. Please try again in a minute."))
        except openai.BadRequestError as exc:
            logger.error("OpenAI API rejected request: %s", exc.message)
            raise AIError()
        except openai.APIStatusError as exc:
            logger.error("OpenAI API error %s (request %s)", exc.status_code, getattr(exc, "request_id", None))
            raise AIUnavailable() if exc.status_code >= 500 else AIError()
        except openai.APIConnectionError:
            logger.warning("Could not reach OpenAI API")
            raise AIUnavailable()

        parts = [part for item in response.output if item.type == "message" for part in item.content]
        if any(part.type == "refusal" for part in parts):
            raise AIError(_("The assistant can't help with this request."))
        text = "".join(part.text for part in parts if part.type == "output_text").strip()
        if not text:
            if response.status == "incomplete":
                logger.warning("OpenAI response incomplete: %s", getattr(response.incomplete_details, "reason", None))
            raise AIError()
        usage = response.usage
        return Completion(
            text=text,
            model=response.model,
            input_tokens=(usage.input_tokens if usage else 0) or 0,
            output_tokens=(usage.output_tokens if usage else 0) or 0,
        )
