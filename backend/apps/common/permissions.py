from rest_framework import permissions


def is_moderator(user) -> bool:
    return bool(user and user.is_authenticated and (user.is_staff or getattr(user, "role", None) in {"moderator", "admin"}))


class IsModerator(permissions.BasePermission):
    def has_permission(self, request, view):
        return is_moderator(request.user)


class IsModeratorOrReadOnly(permissions.BasePermission):
    """Public read; content management limited to moderators/admins (verified listings)."""

    def has_permission(self, request, view):
        return request.method in permissions.SAFE_METHODS or is_moderator(request.user)


class IsOwnerOrReadOnly(permissions.BasePermission):
    """Object-level ownership check. Views declare `owner_field` (default: "owner")."""

    def has_object_permission(self, request, view, obj):
        if request.method in permissions.SAFE_METHODS:
            return True
        owner_field = getattr(view, "owner_field", "owner")
        return getattr(obj, f"{owner_field}_id", None) == request.user.id
