"""Plain-text helpers for story fields."""
import re

# A Markdown heading marker at the start ("# ", "## "...). "C# basics" or "#1 tip" are not headings.
_HEADING = re.compile(r"^\s{0,3}#{1,6}\s+")
# Paired emphasis/code markers that never belong in a plain-text title.
_MARKERS = re.compile(r"\*\*|__|~~|`")


def clean_title(value: str) -> str:
    """Titles are plain text (only the body is Markdown), so strip formatting people paste in.

    "# **From Kakuma to the Open Seas**" -> "From Kakuma to the Open Seas"
    """
    text = _HEADING.sub("", value or "")
    text = _MARKERS.sub("", text).strip()
    # A whole title wrapped in single * or _ (italics).
    for marker in "*_":
        if len(text) > 2 and text.startswith(marker) and text.endswith(marker):
            text = text[1:-1].strip()
    return " ".join(text.split())
