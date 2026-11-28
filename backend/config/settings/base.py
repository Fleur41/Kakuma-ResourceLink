"""Settings shared by every environment. Secrets come from environment variables only."""
from datetime import timedelta
from pathlib import Path

import environ
from django.utils.translation import gettext_lazy as _

BASE_DIR = Path(__file__).resolve().parent.parent.parent

env = environ.Env()
environ.Env.read_env(BASE_DIR / ".env", overwrite=False)

SECRET_KEY = env("DJANGO_SECRET_KEY")
DEBUG = env.bool("DJANGO_DEBUG", default=False)
ALLOWED_HOSTS = env.list("DJANGO_ALLOWED_HOSTS", default=[])

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    # Third party
    "rest_framework",
    "rest_framework_simplejwt.token_blacklist",
    "django_filters",
    "corsheaders",
    "drf_spectacular",
    # Local
    "apps.common",
    "apps.accounts",
    "apps.opportunities",
    "apps.services",
    "apps.stories",
    "apps.diaspora",
    "apps.mentorship",
    "apps.contributions",
    "apps.notifications",
    "apps.moderation",
    "apps.ai_assistant",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "whitenoise.middleware.WhiteNoiseMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.locale.LocaleMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# PostgreSQL is the only supported primary database.
DATABASES = {"default": env.db("DATABASE_URL")}
DATABASES["default"]["ATOMIC_REQUESTS"] = False
DATABASES["default"]["CONN_MAX_AGE"] = env.int("DB_CONN_MAX_AGE", default=60)

AUTH_USER_MODEL = "accounts.User"
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator", "OPTIONS": {"min_length": 8}},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

# Internationalisation — the five platform languages.
LANGUAGE_CODE = "en"
LANGUAGES = [
    ("en", _("English")),
    ("sw", _("Swahili")),
    ("es", _("Spanish")),
    ("it", _("Italian")),
    ("de", _("German")),
]
LANGUAGE_CODES = [code for code, _name in LANGUAGES]
LOCALE_PATHS = [BASE_DIR / "locale"]
TIME_ZONE = "Africa/Nairobi"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
MEDIA_URL = "/media/"
MEDIA_ROOT = Path(env("MEDIA_ROOT", default=str(BASE_DIR / "media")))
# Let Django serve uploads when DEBUG is off (single-server hosting without a CDN or object storage).
SERVE_MEDIA = env.bool("SERVE_MEDIA", default=False)

# Upload limits (bytes). Enforced by apps.common.validators and Django itself.
MAX_IMAGE_UPLOAD_BYTES = env.int("MAX_IMAGE_UPLOAD_BYTES", default=5 * 1024 * 1024)
DATA_UPLOAD_MAX_MEMORY_SIZE = 10 * 1024 * 1024
FILE_UPLOAD_MAX_MEMORY_SIZE = 5 * 1024 * 1024

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": ["rest_framework.permissions.IsAuthenticatedOrReadOnly"],
    "DEFAULT_PAGINATION_CLASS": "apps.common.pagination.StandardPagination",
    "PAGE_SIZE": 20,
    "DEFAULT_FILTER_BACKENDS": [
        "django_filters.rest_framework.DjangoFilterBackend",
        "rest_framework.filters.SearchFilter",
        "rest_framework.filters.OrderingFilter",
    ],
    "DEFAULT_SCHEMA_CLASS": "drf_spectacular.openapi.AutoSchema",
    "EXCEPTION_HANDLER": "apps.common.exceptions.api_exception_handler",
    "DEFAULT_THROTTLE_CLASSES": [
        "rest_framework.throttling.AnonRateThrottle",
        "rest_framework.throttling.UserRateThrottle",
    ],
    "DEFAULT_THROTTLE_RATES": {
        "anon": env("THROTTLE_ANON", default="120/min"),
        "user": env("THROTTLE_USER", default="600/min"),
        "auth": env("THROTTLE_AUTH", default="10/min"),
        "ai": env("THROTTLE_AI", default="20/hour"),
    },
    "TEST_REQUEST_DEFAULT_FORMAT": "json",
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=env.int("JWT_ACCESS_MINUTES", default=15)),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=env.int("JWT_REFRESH_DAYS", default=7)),
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "UPDATE_LAST_LOGIN": True,
    "AUTH_HEADER_TYPES": ("Bearer",),
    "USER_ID_FIELD": "id",
    "USER_ID_CLAIM": "user_id",
}

SPECTACULAR_SETTINGS = {
    "TITLE": "Kakuma ResourceLink API",
    "DESCRIPTION": "Connecting Opportunities, Inspiring Generations, Transforming Kakuma.",
    "VERSION": "1.0.0",
    "SERVE_INCLUDE_SCHEMA": False,
    "SCHEMA_PATH_PREFIX": r"/api/v1",
    "COMPONENT_SPLIT_REQUEST": True,
    # Stable enum names so generated Android/React types are readable.
    "ENUM_NAME_OVERRIDES": {
        "LanguageEnum": "apps.accounts.models.Language",
        "UserRoleEnum": "apps.accounts.models.Role",
        "OpportunityCategoryEnum": "apps.opportunities.models.OpportunityCategory",
        "OpportunityStatusEnum": "apps.opportunities.models.OpportunityStatus",
        "ServiceCategoryEnum": "apps.services.models.ServiceCategory",
        "StoryCategoryEnum": "apps.stories.models.StoryCategory",
        "StoryStatusEnum": "apps.stories.models.StoryStatus",
        "AlumniVisibilityEnum": "apps.diaspora.models.Visibility",
        "MentorshipStatusEnum": "apps.mentorship.models.MentorshipStatus",
        "HelpAreaEnum": "apps.mentorship.models.HelpArea",
        "ProjectCategoryEnum": "apps.contributions.models.ProjectCategory",
        "ProjectStatusEnum": "apps.contributions.models.ProjectStatus",
        "ContributionTypeEnum": "apps.contributions.models.ContributionType",
        "ContributionStatusEnum": "apps.contributions.models.ContributionStatus",
        "NotificationTypeEnum": "apps.notifications.models.NotificationType",
        "ReportTargetTypeEnum": "apps.moderation.models.TargetType",
        "ReportReasonEnum": "apps.moderation.models.ReportReason",
        "ReportStatusEnum": "apps.moderation.models.ReportStatus",
    },
}

CORS_ALLOWED_ORIGINS = env.list("CORS_ALLOWED_ORIGINS", default=[])
CSRF_TRUSTED_ORIGINS = env.list("CSRF_TRUSTED_ORIGINS", default=[])

EMAIL_BACKEND = env("EMAIL_BACKEND", default="django.core.mail.backends.console.EmailBackend")
# SMTP (used when EMAIL_BACKEND is django.core.mail.backends.smtp.EmailBackend), e.g. Gmail with an app password.
EMAIL_HOST = env("EMAIL_HOST", default="smtp.gmail.com")
EMAIL_PORT = env.int("EMAIL_PORT", default=587)
EMAIL_HOST_USER = env("EMAIL_HOST_USER", default="")
EMAIL_HOST_PASSWORD = env("EMAIL_HOST_PASSWORD", default="")
EMAIL_USE_TLS = env.bool("EMAIL_USE_TLS", default=True)
EMAIL_USE_SSL = env.bool("EMAIL_USE_SSL", default=False)
EMAIL_TIMEOUT = env.int("EMAIL_TIMEOUT", default=15)
DEFAULT_FROM_EMAIL = env("DEFAULT_FROM_EMAIL", default=EMAIL_HOST_USER or "no-reply@kakuma-resourcelink.local")
# Password reset by emailed 6-digit code.
PASSWORD_RESET_CODE_MINUTES = env.int("PASSWORD_RESET_CODE_MINUTES", default=15)
PASSWORD_RESET_CODE_ATTEMPTS = env.int("PASSWORD_RESET_CODE_ATTEMPTS", default=5)
PASSWORD_RESET_RESEND_SECONDS = env.int("PASSWORD_RESET_RESEND_SECONDS", default=60)

# Stories are reviewed by moderators before publication unless disabled.
STORIES_REQUIRE_REVIEW = env.bool("STORIES_REQUIRE_REVIEW", default=True)
PROJECTS_REQUIRE_REVIEW = env.bool("PROJECTS_REQUIRE_REVIEW", default=True)

# AI assistant — API keys are only ever read on the server.
# openai (ChatGPT models, Responses API) | anthropic (Claude) | ollama (local models, no key)
AI_PROVIDER = env("AI_PROVIDER", default="openai")
OPENAI_API_KEY = env("OPENAI_API_KEY", default="")
ANTHROPIC_API_KEY = env("ANTHROPIC_API_KEY", default="")
OLLAMA_BASE_URL = env("OLLAMA_BASE_URL", default="http://localhost:11434/v1")
# Off by default: thinking makes local models several times slower to reply.
OLLAMA_THINK = env.bool("OLLAMA_THINK", default=False)
# How long Ollama keeps the model in memory after a reply (Ollama's own default is 5m).
OLLAMA_KEEP_ALIVE = env("OLLAMA_KEEP_ALIVE", default="2h")
# Chat answers are short; the cap stops a local model that starts repeating itself.
OLLAMA_MAX_OUTPUT_TOKENS = env.int("OLLAMA_MAX_OUTPUT_TOKENS", default=1024)
_DEFAULT_MODELS = {"openai": "gpt-6.2-sol", "anthropic": "claude-opus-5-5", "ollama": "gemma3:12b"}
AI_MODEL = env("AI_MODEL", default=_DEFAULT_MODELS.get(AI_PROVIDER, ""))
# low | medium | high reasoning effort — chat assistance does well at low/medium effort.
AI_EFFORT = env("AI_EFFORT", default="medium")
# Server-side refusal fallback (anthropic provider only). Disable on Bedrock/Vertex/Foundry.
AI_ENABLE_FALLBACKS = env.bool("AI_ENABLE_FALLBACKS", default=True)
AI_MAX_OUTPUT_TOKENS = env.int("AI_MAX_OUTPUT_TOKENS", default=8000)  # includes reasoning tokens
AI_HISTORY_MESSAGES = env.int("AI_HISTORY_MESSAGES", default=12)
AI_MAX_INPUT_CHARS = env.int("AI_MAX_INPUT_CHARS", default=4000)
# Local models are slower, and the first request also loads the model into memory.
AI_TIMEOUT_SECONDS = env.int("AI_TIMEOUT_SECONDS", default=180 if AI_PROVIDER == "ollama" else 60)

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "root": {"handlers": ["console"], "level": env("LOG_LEVEL", default="INFO")},
}
