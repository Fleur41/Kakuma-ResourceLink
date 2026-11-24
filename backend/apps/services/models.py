from django.db import models
from django.utils.translation import gettext_lazy as _

from apps.common.models import UUIDModel


class ServiceCategory(models.TextChoices):
    HEALTHCARE = "healthcare", _("Healthcare")
    HUMANITARIAN = "humanitarian", _("Humanitarian organization")
    EDUCATION = "education", _("Education")
    LEGAL = "legal", _("Legal aid")
    PROTECTION = "protection", _("Protection")
    LIVELIHOOD = "livelihood", _("Livelihood")
    COMMUNITY = "community", _("Community service")


class Service(UUIDModel):
    name = models.CharField(max_length=200)
    category = models.CharField(max_length=20, choices=ServiceCategory.choices)
    organization = models.CharField(max_length=200, blank=True)
    description = models.TextField()
    phone = models.CharField(max_length=40, blank=True)
    email = models.EmailField(blank=True)
    website = models.URLField(blank=True)
    address = models.CharField(max_length=255, blank=True)
    latitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    longitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    opening_hours = models.CharField(max_length=200, blank=True)
    is_verified = models.BooleanField(default=False)
    last_verified_at = models.DateTimeField(null=True, blank=True)
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ["name"]
        indexes = [models.Index(fields=["category", "is_active"])]

    def __str__(self) -> str:
        return self.name
