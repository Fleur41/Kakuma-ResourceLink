from django.contrib import admin

from .models import Opportunity


@admin.register(Opportunity)
class OpportunityAdmin(admin.ModelAdmin):
    list_display = ["title", "category", "organization", "deadline", "status", "is_verified"]
    list_filter = ["category", "status", "is_verified", "is_remote", "language"]
    search_fields = ["title", "organization"]
