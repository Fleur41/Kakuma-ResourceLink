"""Test settings: PostgreSQL (same engine as production), fast hashing, no throttling surprises."""
from .base import *  # noqa: F401,F403
from .base import REST_FRAMEWORK

PASSWORD_HASHERS = ["django.contrib.auth.hashers.MD5PasswordHasher"]
EMAIL_BACKEND = "django.core.mail.backends.locmem.EmailBackend"
AI_PROVIDER = "openai"
AI_MODEL = "gpt-6.2-sol"
OPENAI_API_KEY = "test-key-not-real"
ANTHROPIC_API_KEY = "test-key-not-real"
STORIES_REQUIRE_REVIEW = True
PROJECTS_REQUIRE_REVIEW = True
MEDIA_ROOT = BASE_DIR / ".test-media"  # noqa: F405

# Keep throttles high in tests; dedicated tests override per-case.
REST_FRAMEWORK = {
    **REST_FRAMEWORK,
    "DEFAULT_THROTTLE_RATES": {"anon": "10000/min", "user": "10000/min", "auth": "10000/min", "ai": "10000/min"},
}
CACHES = {"default": {"BACKEND": "django.core.cache.backends.locmem.LocMemCache"}}
