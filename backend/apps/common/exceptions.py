"""Uniform error envelope for every API error:

    {"error": {"code": "...", "message": "...", "details": {...}}}
"""
import logging

from django.core.exceptions import PermissionDenied, ValidationError as DjangoValidationError
from django.http import Http404
from django.utils.translation import gettext_lazy as _
from rest_framework import exceptions, status
from rest_framework.response import Response
from rest_framework.views import exception_handler

logger = logging.getLogger(__name__)


class ConflictError(exceptions.APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = _("The request conflicts with the current state of the resource.")
    default_code = "conflict"


class InvalidTransition(ConflictError):
    default_detail = _("This status change is not allowed.")
    default_code = "invalid_transition"


class ServiceUnavailable(exceptions.APIException):
    status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    default_detail = _("This service is temporarily unavailable.")
    default_code = "service_unavailable"


def _first_message(detail) -> str:
    if isinstance(detail, list) and detail:
        return _first_message(detail[0])
    if isinstance(detail, dict) and detail:
        return _first_message(next(iter(detail.values())))
    return str(detail)


def api_exception_handler(exc, context):
    if isinstance(exc, DjangoValidationError):
        exc = exceptions.ValidationError(getattr(exc, "message_dict", None) or exc.messages)
    elif isinstance(exc, Http404):
        exc = exceptions.NotFound()
    elif isinstance(exc, PermissionDenied):
        exc = exceptions.PermissionDenied()

    response = exception_handler(exc, context)
    if response is None:
        logger.exception("Unhandled API error", exc_info=exc)
        return Response(
            {"error": {"code": "server_error", "message": str(_("Something went wrong. Please try again.")), "details": {}}},
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    detail = exc.detail if isinstance(exc, exceptions.APIException) else response.data
    if isinstance(exc, exceptions.ValidationError):
        code = "validation_error"
        message = _first_message(detail)
        details = detail if isinstance(detail, dict) else {"non_field_errors": detail}
    else:
        codes = exc.get_codes() if isinstance(exc, exceptions.APIException) else "error"
        if isinstance(codes, dict):  # e.g. simplejwt InvalidToken
            codes = codes.get("code", "error")
        code = codes if isinstance(codes, str) else "error"
        message = detail.get("detail", _first_message(detail)) if isinstance(detail, dict) else _first_message(detail)
        details = {}

    response.data = {"error": {"code": str(code), "message": str(message), "details": details}}
    return response
