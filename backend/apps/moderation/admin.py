from django.contrib import admin

from .models import ModerationDecision, Report


@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ["target_type", "target_id", "reason", "status", "reporter", "created_at"]
    list_filter = ["status", "target_type", "reason"]


@admin.register(ModerationDecision)
class ModerationDecisionAdmin(admin.ModelAdmin):
    list_display = ["target_type", "target_id", "action", "moderator", "created_at"]
    list_filter = ["action", "target_type"]
