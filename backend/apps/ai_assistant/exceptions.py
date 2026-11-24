from django.utils.translation import gettext_lazy as _
from rest_framework import status
from rest_framework.exceptions import APIException


class AIUnavailable(APIException):
    status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    default_detail = _("The assistant is not available right now. Please try again later.")
    default_code = "ai_unavailable"


class AIError(APIException):
    status_code = status.HTTP_502_BAD_GATEWAY
    default_detail = _("The assistant could not answer this request. Please try again.")
    default_code = "ai_error"
