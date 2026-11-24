from django.contrib import admin

from .models import CommunityProject, Contribution


class ContributionInline(admin.TabularInline):
    model = Contribution
    extra = 0


@admin.register(CommunityProject)
class CommunityProjectAdmin(admin.ModelAdmin):
    list_display = ["title", "owner", "category", "status", "created_at"]
    list_filter = ["status", "category"]
    inlines = [ContributionInline]
