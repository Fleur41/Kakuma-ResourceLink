"""Retrieve verified platform records relevant to a question so Claude answers from real data only."""
import re
from dataclasses import dataclass
from functools import reduce
from operator import or_

from django.db.models import Q
from django.utils import timezone

from apps.opportunities.models import Opportunity, OpportunityCategory, OpportunityStatus
from apps.services.models import Service, ServiceCategory
from apps.stories.models import Story, StoryStatus

# Tags we use to fence data in the prompt. User-written text must not be able to close or open them.
_FENCE_TAG = re.compile(r"<\s*/?\s*(platform_content|record|text_to_translate)\b[^>]*>", re.IGNORECASE)


def neutralize(text: str) -> str:
    """Remove prompt fence tags from untrusted text so it stays inside its data block."""
    return _FENCE_TAG.sub("", text)


_WORD = re.compile(r"[^\W\d_]{4,}", re.UNICODE)
STOPWORDS = {
    "what", "where", "when", "which", "with", "from", "that", "this", "have", "there", "about", "would", "could",
    "should", "find", "help", "need", "want", "like", "know", "please", "tell", "show", "some", "your", "more",
    "into", "they", "them", "their", "does", "will", "also", "just", "kakuma", "resourcelink", "platform",
}
# Simple keyword → category hints (English stems; content search covers other languages).
CATEGORY_HINTS = {
    "scholar": OpportunityCategory.SCHOLARSHIP, "universit": OpportunityCategory.EDUCATION,
    "school": OpportunityCategory.EDUCATION, "course": OpportunityCategory.EDUCATION,
    "vocation": OpportunityCategory.VOCATIONAL, "training": OpportunityCategory.VOCATIONAL,
    "job": OpportunityCategory.JOB, "work": OpportunityCategory.JOB, "employ": OpportunityCategory.JOB,
    "intern": OpportunityCategory.INTERNSHIP, "business": OpportunityCategory.ENTREPRENEURSHIP,
    "entrepreneur": OpportunityCategory.ENTREPRENEURSHIP,
}
SERVICE_HINTS = {
    "health": ServiceCategory.HEALTHCARE, "clinic": ServiceCategory.HEALTHCARE, "hospital": ServiceCategory.HEALTHCARE,
    "doctor": ServiceCategory.HEALTHCARE, "legal": ServiceCategory.LEGAL, "lawyer": ServiceCategory.LEGAL,
    "protect": ServiceCategory.PROTECTION, "food": ServiceCategory.HUMANITARIAN, "shelter": ServiceCategory.HUMANITARIAN,
}


@dataclass
class Source:
    type: str
    id: str
    title: str
    text: str

    def ref(self) -> dict:
        return {"type": self.type, "id": self.id, "title": self.title}


def keywords(text: str, limit: int = 8) -> list[str]:
    seen: list[str] = []
    for word in _WORD.findall(text.lower()):
        if word not in STOPWORDS and word not in seen:
            seen.append(word)
    return seen[:limit]


def _any_icontains(fields: list[str], words: list[str]) -> Q:
    return reduce(or_, (Q(**{f"{f}__icontains": w}) for f in fields for w in words), Q(pk__in=[]))


def _verified(flag: bool) -> str:
    return "verified by platform staff" if flag else "NOT verified — confirm details with the organisation"


def opportunity_source(o: Opportunity) -> Source:
    reqs = "; ".join(o.requirements) if o.requirements else "not listed"
    text = (
        f"Opportunity: {o.title}\nCategory: {o.get_category_display()}\nOrganisation: {o.organization}\n"
        f"Status: {_verified(o.is_verified)}\nLocation: {o.location or 'not listed'}{' (remote)' if o.is_remote else ''}\n"
        f"Deadline: {o.deadline.isoformat() if o.deadline else 'not listed'}\nSummary: {o.summary}\n"
        f"Requirements: {reqs}\nHow to apply: {o.application_url or 'see listing'}\nDescription: {o.description[:2000]}"
    )
    return Source("opportunity", str(o.id), o.title, text)


def service_source(s: Service) -> Source:
    text = (
        f"Service: {s.name}\nCategory: {s.get_category_display()}\nOrganisation: {s.organization or 'not listed'}\n"
        f"Status: {_verified(s.is_verified)}\nAddress: {s.address or 'not listed'}\n"
        f"Opening hours: {s.opening_hours or 'not listed'}\nPhone: {s.phone or 'not listed'}\n"
        f"Website: {s.website or 'not listed'}\nDescription: {s.description[:1200]}"
    )
    return Source("service", str(s.id), s.name, text)


def story_source(st: Story, full: bool = False) -> Source:
    body = st.body if full else st.excerpt
    text = f"Story: {st.title}\nAuthor: {st.author.full_name}\nCategory: {st.get_category_display()}\n{body}"
    return Source("story", str(st.id), st.title, text)


def retrieve(query: str, limit_each: int = 4) -> list[Source]:
    words = keywords(query)
    if not words:
        return []
    today = timezone.localdate()

    opp_q = _any_icontains(["title", "summary", "organization", "description"], words)
    cats = {cat for w in words for stem, cat in CATEGORY_HINTS.items() if w.startswith(stem)}
    if cats:
        opp_q |= Q(category__in=cats)
    opportunities = (
        Opportunity.objects.filter(status=OpportunityStatus.PUBLISHED)
        .filter(Q(deadline__isnull=True) | Q(deadline__gte=today))
        .filter(opp_q)
        .order_by("-is_verified", "deadline")[:limit_each]
    )

    svc_q = _any_icontains(["name", "organization", "description"], words)
    svc_cats = {cat for w in words for stem, cat in SERVICE_HINTS.items() if w.startswith(stem)}
    if svc_cats:
        svc_q |= Q(category__in=svc_cats)
    services = Service.objects.filter(is_active=True).filter(svc_q).order_by("-is_verified")[:limit_each]

    stories = (
        Story.objects.filter(status=StoryStatus.PUBLISHED)
        .filter(_any_icontains(["title", "excerpt"], words))
        .select_related("author")
        .order_by("-published_at")[:3]
    )
    return (
        [opportunity_source(o) for o in opportunities]
        + [service_source(s) for s in services]
        + [story_source(s) for s in stories]
    )


def render(sources: list[Source]) -> str:
    if not sources:
        return "<platform_content>\nNo matching platform records were found.\n</platform_content>"
    parts = [f'<record index="{i}">\n{neutralize(s.text)}\n</record>' for i, s in enumerate(sources, 1)]
    return "<platform_content>\n" + "\n".join(parts) + "\n</platform_content>"
