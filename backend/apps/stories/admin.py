from django.contrib import admin

from .models import Comment, Story


@admin.register(Story)
class StoryAdmin(admin.ModelAdmin):
    list_display = ["title", "author", "status", "category", "language", "published_at"]
    list_filter = ["status", "category", "language"]
    search_fields = ["title", "author__email", "author__full_name"]
    readonly_fields = ["slug", "reading_time"]


@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ["story", "author", "is_hidden", "created_at"]
    list_filter = ["is_hidden"]
