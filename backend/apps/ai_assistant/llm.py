"""Provider-neutral pieces of the assistant: the completion result and the client factory.

The rest of the app only calls ``get_client().complete(system=..., messages=...)``; which AI provider
answers is decided by ``AI_PROVIDER`` (``openai``, ``anthropic`` or ``ollama``).
"""
from dataclasses import dataclass
from typing import Protocol

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured


@dataclass(frozen=True)
class Completion:
    text: str
    model: str
    input_tokens: int
    output_tokens: int


class LLMClient(Protocol):
    def complete(self, *, system: str, messages: list[dict]) -> Completion: ...


def get_client() -> LLMClient:
    """Build the client for the configured provider. Raises AIUnavailable when its API key is missing."""
    provider = settings.AI_PROVIDER
    if provider == "openai":
        from .openai_client import OpenAIClient

        return OpenAIClient()
    if provider == "anthropic":
        from .claude_client import ClaudeClient

        return ClaudeClient()
    if provider == "ollama":
        from .ollama_client import OllamaClient

        return OllamaClient()
    raise ImproperlyConfigured(f"Unknown AI_PROVIDER {provider!r}; use 'openai', 'anthropic' or 'ollama'.")
