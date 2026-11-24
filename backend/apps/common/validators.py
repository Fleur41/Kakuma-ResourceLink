from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator
from django.utils.translation import gettext_lazy as _

ALLOWED_IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "webp"]
ALLOWED_IMAGE_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}

image_extension_validator = FileExtensionValidator(allowed_extensions=ALLOWED_IMAGE_EXTENSIONS)


def validate_image_size(file) -> None:
    limit = settings.MAX_IMAGE_UPLOAD_BYTES
    if file.size > limit:
        raise ValidationError(
            _("Image is too large. Maximum size is %(mb)s MB."), params={"mb": limit // (1024 * 1024)}
        )


def validate_image_content_type(file) -> None:
    # Only uploaded files carry a client content type; stored files are skipped.
    content_type = getattr(file, "content_type", None)
    if content_type and content_type not in ALLOWED_IMAGE_CONTENT_TYPES:
        raise ValidationError(_("Unsupported image type. Use JPG, PNG or WEBP."))


IMAGE_VALIDATORS = [image_extension_validator, validate_image_size, validate_image_content_type]
