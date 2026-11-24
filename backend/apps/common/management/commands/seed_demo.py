"""Seed illustrative demo data for the hackathon.

Every record is SAMPLE content: organisations are generic, nothing is marked verified,
and people are fictional. Replace with verified content before any real launch.
"""
from datetime import timedelta
from decimal import Decimal

from django.conf import settings
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from apps.accounts.models import Role, User
from apps.contributions.models import CommunityProject, ProjectStatus
from apps.diaspora.models import AlumniProfile
from apps.opportunities.models import Opportunity
from apps.services.models import Service
from apps.stories.models import Story, StoryStatus

DEMO_PASSWORD = "Admin@123"  # test-only accounts on a local database; never seed production


class Command(BaseCommand):
    help = "Create sample demo data (idempotent). Demo password for all accounts: " + DEMO_PASSWORD

    @transaction.atomic
    def handle(self, *args, **options):
        if not settings.DEBUG:
            raise CommandError("seed_demo only runs with DEBUG=True (local development databases).")
        today = timezone.localdate()
        users = {}
        for key, email, name, role in [
            ("amina", "amina@demo.kakuma.test", "Amina Hassan", Role.MEMBER),
            ("daniel", "daniel@demo.kakuma.test", "Daniel M.", Role.MENTOR),
            ("grace", "grace@demo.kakuma.test", "Grace A.", Role.ALUMNI),
            ("peter", "peter@demo.kakuma.test", "Peter K.", Role.MENTOR),
            ("mod", "moderator@demo.kakuma.test", "Demo Moderator", Role.MODERATOR),
        ]:
            user, created = User.objects.get_or_create(email=email, defaults={"full_name": name, "role": role})
            if created:
                user.set_password(DEMO_PASSWORD)
                user.save()
            users[key] = user

        alumni = [
            ("daniel", "Germany", "Software engineer", ["Android", "Python", "Career mentoring"], True),
            ("grace", "Canada", "Public health researcher", ["Research", "Education", "Scholarships"], False),
            ("peter", "Kenya", "Entrepreneur", ["Business", "Leadership", "Startups"], True),
        ]
        for key, country, profession, skills, mentor in alumni:
            AlumniProfile.objects.get_or_create(
                user=users[key],
                defaults=dict(
                    country=country, profession=profession, skills=skills, is_mentor=mentor,
                    mentorship_areas=["career", "technical"] if mentor else [], visibility="public",
                    bio="Sample alumni profile for the demo.", consent_given_at=timezone.now(),
                ),
            )

        opportunities = [
            ("Undergraduate scholarship (sample)", "scholarship", "Education partner (sample)", "Tuition support · Applications open", 30,
             ["Secondary school certificate", "Good academic record"]),
            ("Digital skills fellowship (sample)", "education", "Learning organisation (sample)", "Remote learning · 12 weeks", 45,
             ["Basic English", "Access to a phone or computer"]),
            ("Community internship (sample)", "internship", "Local organisation (sample)", "Paid placement · 3 months", 20,
             ["Aged 18-30"]),
            ("Vocational training: solar installation (sample)", "vocational", "Training centre (sample)", "Hands-on · 8 weeks", 60, []),
            ("Junior software developer role (sample)", "job", "Tech employer (sample)", "Remote · Full time", 25,
             ["Portfolio of projects"]),
            ("Small business starter grant (sample)", "entrepreneurship", "Enterprise programme (sample)", "Seed support for new businesses", 40,
             ["Business idea summary"]),
        ]
        for title, cat, org, summary, days, reqs in opportunities:
            Opportunity.objects.get_or_create(
                title=title,
                defaults=dict(
                    category=cat, organization=org, summary=summary, requirements=reqs,
                    description=f"{summary}. This is illustrative sample content for the hackathon demo, not a real listing.",
                    location="Kakuma" if cat in {"vocational", "internship"} else "Online",
                    is_remote=cat in {"education", "job"}, deadline=today + timedelta(days=days), is_verified=False,
                ),
            )

        for name, cat, desc in [
            ("Community health clinic (sample)", "healthcare", "General outpatient care. Sample listing."),
            ("Legal aid help desk (sample)", "legal", "Information on documentation and legal support. Sample listing."),
            ("Youth learning centre (sample)", "education", "Study space and digital literacy classes. Sample listing."),
            ("Livelihood support office (sample)", "livelihood", "Business skills and savings groups. Sample listing."),
        ]:
            Service.objects.get_or_create(
                name=name, defaults=dict(category=cat, description=desc, address="Kakuma (sample)", opening_hours="Mon–Fri 08:00–17:00")
            )

        stories = [
            ("daniel", "From Kakuma to a career in technology", "career",
             "I still remember the first time I used a computer in Kakuma. I had questions, curiosity and a dream "
             "that felt bigger than my circumstances.\n\nThe journey was not simple. I found mentors, applied for "
             "learning opportunities and kept building my skills one step at a time.\n\n## What helped me\n\n"
             "- Free online courses\n- A mentor who answered my questions\n- Friends who studied with me"),
            ("grace", "The teacher who changed my path", "education",
             "Education gave me a language for imagining possibilities. One teacher believed in me before I "
             "believed in myself.\n\nToday I research public health, and I still use the study habits she taught me."),
            ("peter", "Why I mentor young people", "inspiration",
             "I remember the questions I had. Now I make time to answer them.\n\nMentoring is not about having all "
             "the answers — it is about walking alongside someone while they find theirs."),
        ]
        for key, title, cat, body in stories:
            Story.objects.get_or_create(
                title=title,
                defaults=dict(author=users[key], body=body, category=cat, consent_to_publish=True,
                              status=StoryStatus.PUBLISHED, published_at=timezone.now()),
            )

        CommunityProject.objects.get_or_create(
            title="Solar study lamps for students (sample)",
            defaults=dict(
                owner=users["peter"], summary="Help students study after dark.",
                description="Sample project: collect solar lamps and run a study club.", category="education",
                status=ProjectStatus.ACTIVE, funding_goal=Decimal("500"), currency="USD",
                accepted_contribution_types=["mentoring", "volunteering", "equipment", "financial_pledge"],
            ),
        )
        self.stdout.write(self.style.SUCCESS(f"Demo data ready. Accounts use password: {DEMO_PASSWORD}"))
        for u in users.values():
            self.stdout.write(f"  {u.email} ({u.role})")
