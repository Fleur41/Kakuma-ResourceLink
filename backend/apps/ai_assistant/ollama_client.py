"""Local models through Ollama's OpenAI-compatible Chat Completions endpoint. No API key, no data leaves the server."""
import logging
import re

import openai
from django.conf import settings
from django.utils.translation import gettext_lazy as _

from .exceptions import AIError, AIUnavailable
from .llm import Completion

logger = logging.getLogger(__name__)

# Some local models (Qwen, DeepSeek…) put their reasoning inline; it must never reach the user.
_THINK = re.compile(r"<think>.*?</think>", re.DOTALL | re.IGNORECASE)


class OllamaClient:
    def __init__(self, base_url: str | None = None):
        # Ollama ignores the key, but the SDK requires a non-empty one.
        self._client = openai.OpenAI(
            base_url=base_url or settings.OLLAMA_BASE_URL, api_key="ollama",
            timeout=settings.AI_TIMEOUT_SECONDS, max_retries=1,
        )

    def complete(self, *, system: str, messages: list[dict]) -> Completion:
        try:
            response = self._client.chat.completions.create(
                model=settings.AI_MODEL,
                messages=[{"role": "system", "content": system}, *messages],
                max_tokens=settings.AI_MAX_OUTPUT_TOKENS,
            )
        except openai.APIConnectionError:
            # Includes timeouts: the model may still be loading, or Ollama isn't running.
            logger.warning("Could not reach Ollama at %s — is `ollama serve` running?", settings.OLLAMA_BASE_URL)
            raise AIUnavailable()
        except openai.NotFoundError:
            logger.error("Ollama has no model %r — run `ollama pull %s`", settings.AI_MODEL, settings.AI_MODEL)
            raise AIUnavailable()
        except openai.APIStatusError as exc:
            logger.error("Ollama error %s: %s", exc.status_code, exc.message)
            raise AIUnavailable() if exc.status_code >= 500 else AIError()

        choice = response.choices[0] if response.choices else None
        if choice is not None and getattr(choice.message, "refusal", None):
            raise AIError(_("The assistant can't help with this request."))
        text = _THINK.sub("", (choice.message.content if choice else None) or "").strip()
        if not text:
            raise AIError()
        usage = response.usage
        return Completion(
            text=text,
            model=response.model or settings.AI_MODEL,
            input_tokens=(usage.prompt_tokens if usage else 0) or 0,
            output_tokens=(usage.completion_tokens if usage else 0) or 0,
        )
