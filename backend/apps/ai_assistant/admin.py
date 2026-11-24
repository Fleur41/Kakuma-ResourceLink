from django.contrib import admin

from .models import AIConversation, AIMessage


class AIMessageInline(admin.TabularInline):
    model = AIMessage
    extra = 0
    readonly_fields = ["role", "content", "model", "input_tokens", "output_tokens", "created_at"]


@admin.register(AIConversation)
class AIConversationAdmin(admin.ModelAdmin):
    list_display = ["user", "title", "language", "updated_at"]
    inlines = [AIMessageInline]
