import math
import re

_WORD = re.compile(r"\w+", re.UNICODE)


def reading_time_minutes(text: str, wpm: int = 200) -> int:
    return max(1, math.ceil(len(_WORD.findall(text or "")) / wpm))
