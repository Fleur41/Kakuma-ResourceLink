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
    """Also serves ``AI_PROVIDER=compatible``: any hosted OpenAI-compatible Chat Completions API (see below)."""

    _name = "Ollama"
    _ollama_options = True

    def __init__(self, base_url: str | None = None, api_key: str = "ollama"):
        # Ollama ignores the key, but the SDK requires a non-empty one.
        self._base_url = base_url or settings.OLLAMA_BASE_URL
        self._client = openai.OpenAI(
            base_url=self._base_url, api_key=api_key,
            timeout=settings.AI_TIMEOUT_SECONDS, max_retries=1,
        )

    def _extra_options(self) -> dict:
        if not self._ollama_options:
            return {}
        return {
            # Thinking makes qwen3-style models 3-5x slower to answer, which chat users feel most on phones.
            **({} if settings.OLLAMA_THINK else {"reasoning_effort": "none"}),
            # Keep the model in memory between messages so a reply doesn't wait for it to reload.
            "extra_body": {"keep_alive": settings.OLLAMA_KEEP_ALIVE},
        }

    def complete(self, *, system: str, messages: list[dict]) -> Completion:
        try:
            response = self._client.chat.completions.create(
                model=settings.AI_MODEL,
                messages=[{"role": "system", "content": system}, *messages],
                # A small local model can get stuck repeating itself; the cap stops that within seconds, not minutes.
                max_tokens=settings.OLLAMA_MAX_OUTPUT_TOKENS,
                **self._extra_options(),
            )
        except openai.APIConnectionError:
            # Includes timeouts: the model may still be loading, or the server isn't running.
            logger.warning("Could not reach %s at %s", self._name, self._base_url)
            raise AIUnavailable()
        except openai.NotFoundError:
            logger.error("%s has no model %r", self._name, settings.AI_MODEL)
            raise AIUnavailable()
        except (openai.AuthenticationError, openai.PermissionDeniedError) as exc:
            logger.error("%s rejected the API key (%s): %s", self._name, exc.status_code, exc.message)
            raise AIUnavailable()
        except openai.RateLimitError:
            raise AIUnavailable(_("The assistant is busy right now. Please try again in a minute."))
        except openai.APIStatusError as exc:
            logger.error("%s error %s: %s", self._name, exc.status_code, exc.message)
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


class CompatibleClient(OllamaClient):
    """A hosted OpenAI-compatible Chat Completions API (Google Gemini, Groq, OpenRouter…), for servers without Ollama."""

    _name = "AI provider"
    _ollama_options = False

    def __init__(self):
        if not (settings.AI_BASE_URL and settings.AI_API_KEY):
            raise AIUnavailable()
        super().__init__(base_url=settings.AI_BASE_URL, api_key=settings.AI_API_KEY)
