from rest_framework.pagination import PageNumberPagination


class StandardPagination(PageNumberPagination):
    """`?page=N&page_size=M` → {count, next, previous, results}. Works with Android Paging 3 and React."""

    page_size = 20
    page_size_query_param = "page_size"
    max_page_size = 50
