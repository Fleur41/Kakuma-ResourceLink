from django.contrib import admin

from .models import AlumniProfile


@admin.register(AlumniProfile)
class AlumniProfileAdmin(admin.ModelAdmin):
    list_display = ["user", "country", "profession", "is_mentor", "visibility"]
    list_filter = ["is_mentor", "visibility", "country"]
    search_fields = ["user__full_name", "user__email", "profession"]
