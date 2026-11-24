from rest_framework.throttling import AnonRateThrottle, UserRateThrottle


class AuthRateThrottle(AnonRateThrottle):
    """Per-IP throttle for login/registration/reset — applies to anonymous and signed-in callers."""

    scope = "auth"

    def get_cache_key(self, request, view):
        return self.cache_format % {"scope": self.scope, "ident": self.get_ident(request)}


class AIRateThrottle(UserRateThrottle):
    scope = "ai"
