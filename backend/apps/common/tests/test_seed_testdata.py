import pytest
from django.core.management import CommandError, call_command

from apps.accounts.models import User
from apps.mentorship.models import MentorshipRequest
from apps.stories.models import Story

pytestmark = pytest.mark.django_db


def test_refuses_without_debug(settings):
    settings.DEBUG = False
    with pytest.raises(CommandError):
        call_command("seed_testdata")


def test_seeds_every_account_with_one_password_and_is_idempotent(settings):
    settings.DEBUG = True
    call_command("seed_testdata", password="Admin@123")
    counts = (User.objects.count(), Story.objects.count(), MentorshipRequest.objects.count())
    call_command("seed_testdata", password="Admin@123")
    assert (User.objects.count(), Story.objects.count(), MentorshipRequest.objects.count()) == counts

    test_users = User.objects.filter(email__iendswith="kakuma.test")
    assert test_users.count() == 18
    assert all(u.check_password("Admin@123") for u in test_users)
    admin = User.objects.get(email="admin@kakuma.test")
    assert admin.is_superuser and admin.is_staff
    assert set(Story.objects.values_list("status", flat=True)) >= {"published", "pending_review", "draft", "rejected"}
