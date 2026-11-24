import django_filters
from django.db.models import Q
from django.utils import timezone

from .models import Opportunity


class OpportunityFilter(django_filters.FilterSet):
    deadline_after = django_filters.DateFilter(field_name="deadline", lookup_expr="gte")
    deadline_before = django_filters.DateFilter(field_name="deadline", lookup_expr="lte")
    open = django_filters.BooleanFilter(method="filter_open", label="Only open (deadline today or later, or none)")
    bookmarked = django_filters.BooleanFilter(method="filter_bookmarked")

    class Meta:
        model = Opportunity
        fields = ["category", "is_remote", "language", "is_verified"]

    def filter_open(self, queryset, name, value):
        if value:
            return queryset.filter(Q(deadline__isnull=True) | Q(deadline__gte=timezone.localdate()))
        return queryset

    def filter_bookmarked(self, queryset, name, value):
        user = self.request.user
        if value and user.is_authenticated:
            return queryset.filter(bookmarks__user=user)
        return queryset
