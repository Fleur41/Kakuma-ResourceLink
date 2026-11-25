"""Strip Markdown formatting from existing story titles (titles are plain text; only the body is Markdown).

The cleaning rules are copied here on purpose so later changes to apps.stories.text don't alter this migration.
Only the title changes; slugs are left as they are so existing links keep working.
"""
import re

from django.db import migrations

_HEADING = re.compile(r"^\s{0,3}#{1,6}\s+")
_MARKERS = re.compile(r"\*\*|__|~~|`")


def _clean(value):
    text = _MARKERS.sub("", _HEADING.sub("", value or "")).strip()
    for marker in "*_":
        if len(text) > 2 and text.startswith(marker) and text.endswith(marker):
            text = text[1:-1].strip()
    return " ".join(text.split())


def clean_titles(apps, schema_editor):
    Story = apps.get_model("stories", "Story")
    for story in Story.objects.only("id", "title").iterator():
        cleaned = _clean(story.title)
        if cleaned != story.title and len(cleaned) >= 3:
            Story.objects.filter(pk=story.pk).update(title=cleaned)


class Migration(migrations.Migration):
    dependencies = [("stories", "0002_story_title_400")]

    operations = [migrations.RunPython(clean_titles, migrations.RunPython.noop)]
