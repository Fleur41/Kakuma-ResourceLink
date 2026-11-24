import uuid
from pathlib import Path

from django.utils.deconstruct import deconstructible


@deconstructible
class RandomizedUploadTo:
    """Store uploads under random names so user-provided filenames never reach the filesystem."""

    def __init__(self, prefix: str):
        self.prefix = prefix

    def __call__(self, _instance, filename: str) -> str:
        ext = Path(filename).suffix.lower()[:5]
        return f"{self.prefix}/{uuid.uuid4().hex}{ext}"

    def __eq__(self, other):
        return isinstance(other, RandomizedUploadTo) and other.prefix == self.prefix


def randomized_upload_to(prefix: str) -> RandomizedUploadTo:
    return RandomizedUploadTo(prefix)
