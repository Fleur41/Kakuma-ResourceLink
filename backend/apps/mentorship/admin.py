from django.contrib import admin

from .models import MentorshipRequest


@admin.register(MentorshipRequest)
class MentorshipRequestAdmin(admin.ModelAdmin):
    list_display = ["mentee", "mentor", "goal", "status", "created_at"]
    list_filter = ["status"]
