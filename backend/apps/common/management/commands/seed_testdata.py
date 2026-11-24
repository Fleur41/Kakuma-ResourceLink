"""Seed a full, realistic-looking TEST dataset for manual testing on a local database.

    python manage.py seed_testdata                 # every test account gets the password Admin@123
    python manage.py seed_testdata --password X    # or choose another one

Everything is fictional test content: people, organisations, phone numbers (+254 700 000 xxx) and
e-mails (@kakuma.test) are made up. A few listings are marked "verified" only so the badge can be
tested. The command refuses to run when DEBUG is off, so it never touches a production database.
Running it again is safe: existing records are kept, and missing ones are added.
"""
from datetime import timedelta
from decimal import Decimal

from django.conf import settings
from django.core.management import call_command
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.db.models import Q
from django.utils import timezone
from django.utils.translation import gettext_lazy as _

from apps.accounts.models import Role, User
from apps.contributions.models import CommunityProject, Contribution, ContributionStatus, ProjectStatus
from apps.diaspora.models import AlumniProfile
from apps.mentorship.models import MentorshipRequest, MentorshipStatus
from apps.notifications.models import Notification, NotificationType
from apps.notifications.services import notify
from apps.opportunities.models import Opportunity, OpportunityBookmark
from apps.services.models import Service
from apps.stories.models import Comment, Story, StoryBookmark, StoryLike, StoryStatus

from .seed_demo import DEMO_PASSWORD

# Accounts created by seed_demo (@demo.kakuma.test) and this command (@kakuma.test).
TEST_EMAILS = Q(email__iendswith="@kakuma.test") | Q(email__iendswith=".kakuma.test")

# key, email, full name, role, language, location, bio
USERS = [
    ("admin", "admin@kakuma.test", "Platform Admin", Role.ADMIN, "en", "Nairobi", "Administers the platform."),
    ("esther", "esther.wanjiru@kakuma.test", "Esther Wanjiru", Role.MODERATOR, "sw", "Kakuma", "Community moderator."),
    ("deng", "deng.majok@kakuma.test", "Deng Majok", Role.MEMBER, "en", "Kakuma 1",
     "Form four leaver looking for scholarships in engineering."),
    ("halima", "halima.abdi@kakuma.test", "Halima Abdi", Role.MEMBER, "sw", "Kalobeyei",
     "Tailor and mother of three. Learning to run a small business."),
    ("nyabol", "nyabol.gatluak@kakuma.test", "Nyabol Gatluak", Role.MEMBER, "sw", "Kakuma 3",
     "Teaching assistant at a primary school. Interested in teacher training."),
    ("ahmed", "ahmed.yusuf@kakuma.test", "Ahmed Yusuf", Role.MEMBER, "en", "Kakuma 2",
     "Self-taught web developer. Builds websites for local shops."),
    ("sara", "sara.tesfaye@kakuma.test", "Sara Tesfaye", Role.MEMBER, "en", "Kakuma 4",
     "Nursing student and youth group volunteer."),
    ("jean", "jean.habimana@kakuma.test", "Jean-Paul Habimana", Role.MEMBER, "en", "Kalobeyei",
     "Solar technician apprentice."),
    ("achol", "achol.deng@kakuma.test", "Achol Deng", Role.MENTOR, "it", "Turin", "Nurse in Italy. Happy to talk about nursing studies."),
    ("abdi", "abdirahman.hassan@kakuma.test", "Abdirahman Hassan", Role.MENTOR, "de", "Munich",
     "Data analyst. I mentor young people who want to learn data skills."),
    ("joseph", "joseph.lokwang@kakuma.test", "Joseph Lokwang", Role.ALUMNI, "es", "Valencia", "Civil engineer in Spain."),
    ("mary", "mary.nyandeng@kakuma.test", "Mary Nyandeng", Role.ALUMNI, "en", "Winnipeg", "Social worker supporting newcomers."),
    ("ibrahim", "ibrahim.omar@kakuma.test", "Ibrahim Omar", Role.ALUMNI, "en", "Minneapolis", "Software tester. Profile kept private."),
]

# key, country, city, profession, organisation, skills, mentor, areas, contribute, interests, visibility, show email
ALUMNI = [
    ("achol", "Italy", "Turin", "Registered nurse", "City hospital", ["Nursing", "Italian language", "University applications"],
     True, ["career", "scholarships"], True, ["mentoring", "skills"], "public", True),
    ("abdi", "Germany", "Munich", "Data analyst", "Logistics company", ["SQL", "Python", "Excel", "Data visualisation"],
     True, ["technical", "career"], True, ["mentoring", "equipment"], "public", False),
    ("joseph", "Spain", "Valencia", "Civil engineer", "Construction firm", ["AutoCAD", "Project management", "Spanish language"],
     False, [], True, ["financial_pledge", "skills"], "members", False),
    ("mary", "Canada", "Winnipeg", "Social worker", "Newcomer support centre", ["Counselling", "Case management"],
     True, ["career", "other"], False, [], "members", True),
    ("ibrahim", "United States", "Minneapolis", "Software tester", "", ["QA", "Selenium"], False, [], False, [], "private", False),
]

# title, category, organisation, summary, days to deadline (None = rolling), requirements, remote, verified, language
OPPORTUNITIES = [
    ("Engineering degree scholarship 2027", "scholarship", "Northbridge Education Fund",
     "Full tuition and a monthly stipend for a 4-year engineering degree.", 45,
     ["KCSE mean grade B or above", "Refugee or host-community youth aged 18-25", "Recommendation letter"], False, True, "en"),
    ("Nursing diploma bursary", "scholarship", "Lakeside Health Training College",
     "Covers 70% of fees for a 3-year nursing diploma.", 3, ["KCSE C+ in sciences", "Interview in Kakuma"], False, False, "en"),
    ("Online data analytics course", "education", "Open Skills Academy",
     "12-week online course with mentors. Certificate on completion.", 21, ["Basic English", "Phone or laptop with internet"],
     True, True, "en"),
    ("Mafunzo ya ushonaji na biashara", "vocational", "Kalobeyei Women's Centre",
     "Mafunzo ya miezi 3 ya ushonaji na jinsi ya kuendesha biashara ndogo.", 14, ["Umri wa miaka 18 au zaidi"], False, False, "sw"),
    ("Solar installation technician course", "vocational", "Sunline Training Centre",
     "Hands-on 8-week course. Toolkit provided to graduates.", 30, ["Basic maths"], False, False, "en"),
    ("Junior web developer (remote)", "job", "Brightpath Digital", "Remote, full-time role building websites for NGOs.", 25,
     ["Portfolio of 2+ websites", "HTML, CSS, JavaScript"], True, False, "en"),
    ("Community health promoter", "job", "Turkana Health Partners",
     "Paid role visiting households to share health information.", 10, ["Fluent in Kiswahili", "Secondary school certificate"],
     False, True, "en"),
    ("Teaching assistant internship", "internship", "Kakuma Learning Network", "6-month paid internship in primary schools.", 18,
     ["Interest in teaching", "Available weekdays"], False, False, "en"),
    ("Youth business starter grant", "entrepreneurship", "Horizon Enterprise Programme",
     "Grants of up to USD 500 plus 6 weeks of business training.", 40, ["Business idea", "Aged 18-35"], False, False, "en"),
    ("Beca de formación en ingeniería civil", "scholarship", "Fundación Puentes", "Beca para un curso en línea de ingeniería civil.",
     35, ["Inglés o español básico"], True, False, "es"),
    ("Borsa di studio per infermieri", "scholarship", "Associazione Ponte Salute", "Borsa per un corso di infermieristica online.",
     50, ["Diploma di scuola superiore"], True, False, "it"),
    ("IT-Ausbildung online", "education", "Digitale Brücke e.V.", "Kostenloser Online-Kurs für IT-Grundlagen.", None,
     ["Grundkenntnisse in Englisch"], True, False, "de"),
    ("English for work evening classes", "education", "Kakuma Learning Network",
     "Free evening classes, three times a week. Rolling intake.", None, [], False, False, "en"),
    ("Agribusiness fellowship (closed)", "entrepreneurship", "Greenfields Programme",
     "Applications are closed. Kept to test the 'closed' state.", -5, ["Farming experience"], False, False, "en"),
]

# name, category, organisation, description, phone suffix, hours, verified
SERVICES = [
    ("Kakuma Mission Clinic", "healthcare", "Turkana Health Partners",
     "Outpatient care, vaccinations and maternal health. Free for registered residents.", "101", "Mon–Sat 07:30–17:00", True),
    ("Kalobeyei Health Post", "healthcare", "Turkana Health Partners", "Basic care and referrals for Kalobeyei settlement.", "102",
     "Mon–Fri 08:00–16:00", False),
    ("Legal aid help desk", "legal", "Justice Access Kenya",
     "Help with documents, birth registration and legal questions.", "103", "Tue & Thu 09:00–15:00", True),
    ("Child protection desk", "protection", "Safe Futures", "Confidential support for children and families at risk.", "104",
     "Mon–Fri 08:00–17:00 · emergencies 24/7", False),
    ("Women's safe space", "protection", "Safe Futures", "Counselling and support groups for women and girls.", "105",
     "Mon–Fri 09:00–16:00", False),
    ("Youth learning centre", "education", "Kakuma Learning Network", "Study space, library and digital literacy classes.", "106",
     "Mon–Sat 08:00–20:00", True),
    ("Livelihood support office", "livelihood", "Horizon Enterprise Programme", "Business skills, savings groups and job referrals.",
     "107", "Mon–Fri 08:30–16:30", False),
    ("Food distribution point 2", "humanitarian", "Regional Food Programme", "Monthly food distribution. Bring your ration card.",
     "108", "Distribution days only", False),
    ("Community sports ground", "community", "Kakuma Youth Sports", "Football and volleyball leagues for young people.", "109",
     "Daily 15:00–18:30", False),
    ("Counselling centre", "healthcare", "Mind Well Kenya", "Free, confidential mental health counselling.", "110",
     "Mon–Fri 09:00–17:00", False),
]

# key, author, title, category, language, status, body, rejection reason
STORIES = [
    ("deng_journey", "deng", "Walking 5 km to the library", "education", "en", StoryStatus.PUBLISHED,
     "Every evening I walked to the youth learning centre because it had light and books.\n\n"
     "## What kept me going\n\n- My younger sister asking me to teach her\n- A teacher who lent me past papers\n"
     "- Friends who studied with me\n\nToday I am applying for engineering scholarships.", ""),
    ("halima_business", "halima", "Biashara yangu ya ushonaji", "entrepreneurship", "sw", StoryStatus.PUBLISHED,
     "Nilianza na cherehani moja niliyoazima kutoka kwa jirani.\n\nSasa ninashona sare za shule kwa watoto wa mtaa wetu, "
     "na nimeajiri wanawake wawili.\n\n**Ushauri wangu:** anza kidogo, weka akiba kila wiki.", ""),
    ("achol_nursing", "achol", "Da Kakuma all'ospedale di Torino", "career", "it", StoryStatus.PUBLISHED,
     "Ho studiato infermieristica in Kenya e poi in Italia.\n\nIl primo anno è stato difficile: la lingua, il freddo, "
     "la solitudine. Ma ogni paziente mi ricorda perché ho scelto questo lavoro.", ""),
    ("abdi_data", "abdi", "Von Excel zu Python", "career", "de", StoryStatus.PUBLISHED,
     "In Kakuma habe ich mit Excel auf einem geliehenen Laptop angefangen.\n\nHeute arbeite ich als Datenanalyst in München. "
     "Mein Tipp: Lerne jeden Tag 30 Minuten – auch auf dem Handy.", ""),
    ("joseph_bridges", "joseph", "Construir puentes", "resettlement", "es", StoryStatus.PUBLISHED,
     "Llegué a España sin hablar español.\n\nHoy diseño puentes en Valencia. Lo más importante fue no rendirme "
     "y pedir ayuda cuando la necesitaba.", ""),
    ("mary_advice", "mary", "Five things I wish I knew before resettlement", "advice", "en", StoryStatus.PUBLISHED,
     "1. Keep copies of every document.\n2. Learn the bus routes in your first week.\n3. Ask about free language classes.\n"
     "4. Winter clothes matter more than you think.\n5. Find your community early — you are not alone.", ""),
    ("sara_volunteer", "sara", "What volunteering taught me about nursing", "community", "en", StoryStatus.PUBLISHED,
     "I joined a youth group that visits elderly neighbours.\n\nListening to them made me want to become a nurse.", ""),
    ("ahmed_pending", "ahmed", "How I built my first website on a phone", "career", "en", StoryStatus.PENDING_REVIEW,
     "I did not have a laptop, so I wrote my first HTML page on my phone.\n\nIt took weeks, but the shop owner loved it "
     "and paid me in airtime and lunch.", ""),
    ("nyabol_pending", "nyabol", "Kufundisha watoto 80 darasani", "education", "sw", StoryStatus.PENDING_REVIEW,
     "Darasa letu lina watoto themanini.\n\nNimejifunza kutumia nyimbo na michezo ili kila mtoto ashiriki.", ""),
    ("jean_draft", "jean", "My first solar installation", "career", "en", StoryStatus.DRAFT,
     "Draft: notes about installing a panel on the community hall roof.", ""),
    ("deng_rejected", "deng", "My scholarship interview", "education", "en", StoryStatus.REJECTED,
     "The interview was at the field office. My case number is 123-45-678 and I live in block 7, house 12.",
     "Thank you for sharing! Please remove your case number and exact address to protect your privacy, then resubmit."),
]

# story key, author key, text
COMMENTS = [
    ("deng_journey", "sara", "This is so inspiring. I also studied at the learning centre!"),
    ("deng_journey", "abdi", "Keep going, Deng. Message me when you start your applications."),
    ("halima_business", "nyabol", "Hongera sana Halima! Unanitia moyo."),
    ("mary_advice", "jean", "Number 4 is so true — my cousin told me the same."),
    ("abdi_data", "ahmed", "Which free courses did you use for Python?"),
]

# mentee, mentor, goal, areas, message, status, response
MENTORSHIP = [
    ("deng", "achol", "Apply for nursing or engineering scholarships", ["scholarships", "career"],
     "Hello Achol, I want to apply for scholarships next year and don't know where to start.", MentorshipStatus.PENDING, ""),
    ("ahmed", "abdi", "Learn data analysis with Python", ["technical"],
     "I build websites and want to move into data. Could you guide me?", MentorshipStatus.ACCEPTED,
     "Happy to help! Let's meet on Saturdays at 10:00 Kakuma time."),
    ("sara", "achol", "Prepare for nursing school interviews", ["career"],
     "I have an interview next month for a nursing diploma.", MentorshipStatus.COMPLETED,
     "Good luck Sara, you are ready!"),
    ("halima", "mary", "Grow my tailoring business", ["entrepreneurship"],
     "I want to sell school uniforms to more schools.", MentorshipStatus.DECLINED,
     "I'm sorry, I'm not the best fit for business questions. Try Peter in the directory."),
    ("nyabol", "abdi", "Use Excel for school records", ["technical"], "Could you teach me basic Excel?",
     MentorshipStatus.CANCELLED, ""),
]

# key, owner, title, summary, description, category, status, goal, accepted types
PROJECTS = [
    ("library", "nyabol", "Books for Kakuma 3 primary library", "Stock a small library with 500 storybooks.",
     "Our school has no library. We want shelves, 500 storybooks in English and Kiswahili, and a weekly reading club.",
     "education", ProjectStatus.ACTIVE, Decimal("800"), ["volunteering", "equipment", "financial_pledge"]),
    ("coding", "ahmed", "Weekend coding club", "Teach 30 teenagers to build websites.",
     "We need 5 refurbished laptops, mentors who can join online, and a small budget for internet bundles.",
     "technology", ProjectStatus.ACTIVE, Decimal("1200"), ["mentoring", "equipment", "skills", "financial_pledge"]),
    ("football", "jean", "Girls' football league", "Kits and balls for 8 girls' teams.",
     "Set up a league for girls aged 12–17 with weekly matches and a coaching programme.",
     "youth", ProjectStatus.PENDING_REVIEW, Decimal("600"), ["volunteering", "equipment", "financial_pledge"]),
    ("garden", "halima", "Kitchen gardens for 20 families", "Seeds, tools and training for small gardens.",
     "Completed last season: 20 families now grow vegetables at home.", "environment", ProjectStatus.COMPLETED,
     Decimal("300"), ["volunteering", "equipment"]),
    ("phones", "deng", "Phone charging kiosk", "A solar kiosk for charging phones.", "Not approved in this form; kept for testing.",
     "livelihood", ProjectStatus.REJECTED, None, ["equipment"]),
]

# project key, contributor, type, description, amount, status
CONTRIBUTIONS = [
    ("library", "joseph", "financial_pledge", "Pledge towards the storybooks.", Decimal("150"), ContributionStatus.CONFIRMED),
    ("library", "mary", "volunteering", "Two online read-aloud sessions a month.", None, ContributionStatus.PLEDGED),
    ("library", "achol", "financial_pledge", "For the shelves.", Decimal("80"), ContributionStatus.PLEDGED),
    ("coding", "abdi", "mentoring", "Monthly online session on data basics.", None, ContributionStatus.CONFIRMED),
    ("coding", "abdi", "equipment", "Two refurbished laptops.", None, ContributionStatus.FULFILLED),
    ("coding", "joseph", "financial_pledge", "Internet bundles for 3 months.", Decimal("200"), ContributionStatus.PLEDGED),
    ("garden", "sara", "volunteering", "Helped with training days.", None, ContributionStatus.FULFILLED),
]


class Command(BaseCommand):
    help = "Seed a full fictional test dataset (local databases only). All test accounts get the same password."

    def add_arguments(self, parser):
        parser.add_argument("--password", default=DEMO_PASSWORD, help=f"Password for every test account (default: {DEMO_PASSWORD})")

    def handle(self, *args, **options):
        if not settings.DEBUG:
            raise CommandError("seed_testdata only runs with DEBUG=True (local development databases).")
        call_command("seed_demo", stdout=self.stdout)
        with transaction.atomic():
            users = self._users(options["password"])
            self._alumni(users)
            opportunities = self._opportunities(users)
            self._services()
            stories = self._stories(users)
            self._engagement(users, stories, opportunities)
            self._mentorship(users)
            self._projects(users)
            self._welcome(users)
        self._report(options["password"])

    # --- people --------------------------------------------------------------------------------
    def _users(self, password):
        users = {}
        for key, email, name, role, lang, location, bio in USERS:
            user, created = User.objects.get_or_create(
                email=email, defaults=dict(full_name=name, role=role, preferred_language=lang)
            )
            if created:
                user.date_joined = timezone.now() - timedelta(days=30 + len(users) * 7)
                if role == Role.ADMIN:
                    user.is_staff = user.is_superuser = True
                user.save()
                user.profile.bio, user.profile.location = bio, location
                user.profile.save()
            users[key] = user
        for user in User.objects.filter(email__endswith="@demo.kakuma.test"):
            users[user.email.split("@")[0].replace("moderator", "mod")] = user
        # Every test account, old and new, uses the same password.
        for user in User.objects.filter(TEST_EMAILS):
            user.set_password(password)
            user.save(update_fields=["password"])
        return users

    def _alumni(self, users):
        for key, country, city, prof, org, skills, mentor, areas, contribute, interests, visibility, show_email in ALUMNI:
            AlumniProfile.objects.get_or_create(
                user=users[key],
                defaults=dict(
                    country=country, city=city, profession=prof, organization=org, skills=skills, is_mentor=mentor,
                    mentorship_areas=areas, willing_to_contribute=contribute, contribution_interests=interests,
                    visibility=visibility, show_contact_email=show_email, show_city=True,
                    bio=users[key].profile.bio, consent_given_at=timezone.now(),
                ),
            )

    # --- content -------------------------------------------------------------------------------
    def _opportunities(self, users):
        today = timezone.localdate()
        result = {}
        for title, cat, org, summary, days, reqs, remote, verified, lang in OPPORTUNITIES:
            result[title], _created = Opportunity.objects.get_or_create(
                title=title,
                defaults=dict(
                    category=cat, organization=org, summary=summary, requirements=reqs, is_remote=remote,
                    description=f"{summary}\n\nTest listing: the organisation and details are fictional.",
                    location="Online" if remote else "Kakuma", is_verified=verified, language=lang,
                    deadline=None if days is None else today + timedelta(days=days),
                    application_url="https://example.org/apply", created_by=users["admin"],
                ),
            )
        return result

    def _services(self):
        for name, cat, org, desc, phone, hours, verified in SERVICES:
            Service.objects.get_or_create(
                name=name,
                defaults=dict(
                    category=cat, organization=org, description=desc, phone=f"+254 700 000 {phone}",
                    email=f"info{phone}@kakuma.test", address="Kakuma, Turkana County (test address)", opening_hours=hours,
                    is_verified=verified, last_verified_at=timezone.now() - timedelta(days=12) if verified else None,
                ),
            )

    def _stories(self, users):
        stories = {}
        for i, (key, author, title, cat, lang, status, body, reason) in enumerate(STORIES):
            published = status == StoryStatus.PUBLISHED
            story, created = Story.objects.get_or_create(
                title=title,
                defaults=dict(
                    author=users[author], body=body, category=cat, language=lang, status=status,
                    consent_to_publish=status != StoryStatus.DRAFT, rejection_reason=reason,
                    published_at=timezone.now() - timedelta(days=i * 2) if published else None,
                ),
            )
            stories[key] = story
            if created and published:
                notify(users[author], NotificationType.STORY_PUBLISHED, _("Your story is live"),
                       _("“%(title)s” has been published."), {"story_id": str(story.id)}, params={"title": title})
            if created and status == StoryStatus.REJECTED:
                notify(users[author], NotificationType.STORY_REJECTED, _("Your story needs changes"), reason,
                       {"story_id": str(story.id)})
        return stories

    def _engagement(self, users, stories, opportunities):
        for story_key, author, text in COMMENTS:
            story = stories[story_key]
            _comment, created = Comment.objects.get_or_create(story=story, author=users[author], body=text)
            if created and story.author_id != users[author].id:
                notify(story.author, NotificationType.STORY_COMMENT, _("New comment on your story"),
                       _("%(name)s commented on “%(title)s”."), {"story_id": str(story.id)},
                       params={"name": users[author].full_name, "title": story.title})
        likes = {"deng_journey": ["sara", "abdi", "halima", "amina", "jean"], "halima_business": ["nyabol", "amina"],
                 "mary_advice": ["deng", "jean", "sara"], "abdi_data": ["ahmed"], "achol_nursing": ["sara", "deng"]}
        for story_key, likers in likes.items():
            for liker in likers:
                StoryLike.objects.get_or_create(story=stories[story_key], user=users[liker])
        for reader, keys in {"amina": ["mary_advice", "deng_journey"], "deng": ["abdi_data"]}.items():
            for key in keys:
                StoryBookmark.objects.get_or_create(story=stories[key], user=users[reader])
        for reader, titles in {"amina": ["Engineering degree scholarship 2027", "Online data analytics course"],
                               "deng": ["Engineering degree scholarship 2027", "Nursing diploma bursary"],
                               "ahmed": ["Junior web developer (remote)"]}.items():
            for title in titles:
                OpportunityBookmark.objects.get_or_create(opportunity=opportunities[title], user=users[reader])

    def _mentorship(self, users):
        for mentee, mentor, goal, areas, message, status, response in MENTORSHIP:
            req, created = MentorshipRequest.objects.get_or_create(
                mentee=users[mentee], mentor=users[mentor], goal=goal,
                defaults=dict(help_areas=areas, message=message, status=status, response_message=response,
                              responded_at=None if status in (MentorshipStatus.PENDING, MentorshipStatus.CANCELLED) else timezone.now()),
            )
            if not created:
                continue
            notify(users[mentor], NotificationType.MENTORSHIP_REQUEST, _("New mentorship request"),
                   _("%(name)s would like your mentorship: %(goal)s"), {"request_id": str(req.id)},
                   params={"name": users[mentee].full_name, "goal": goal})
            if status not in (MentorshipStatus.PENDING, MentorshipStatus.CANCELLED):
                notify(users[mentee], NotificationType.MENTORSHIP_UPDATE, _("Mentorship request update"),
                       _("Your mentorship request “%(goal)s” is now %(status)s."),
                       {"request_id": str(req.id), "status": status},
                       params={"goal": goal, "status": MentorshipStatus(status).label})

    def _projects(self, users):
        projects = {}
        for key, owner, title, summary, desc, cat, status, goal, types in PROJECTS:
            projects[key], _created = CommunityProject.objects.get_or_create(
                title=title,
                defaults=dict(owner=users[owner], summary=summary, description=desc, category=cat, status=status,
                              funding_goal=goal, currency="USD", accepted_contribution_types=types),
            )
        for key, contributor, ctype, desc, amount, status in CONTRIBUTIONS:
            project = projects[key]
            _c, created = Contribution.objects.get_or_create(
                project=project, contributor=users[contributor], type=ctype, description=desc,
                defaults=dict(amount=amount, currency="USD" if amount else "", status=status),
            )
            if created:
                notify(project.owner, NotificationType.PROJECT_CONTRIBUTION, _("New contribution pledged"),
                       _("%(name)s pledged support to “%(title)s”."), {"project_id": str(project.id)},
                       params={"name": users[contributor].full_name, "title": project.title})

    def _welcome(self, users):
        # One older notification per member, already read, so the read/unread states can be tested.
        for user in users.values():
            if not Notification.objects.filter(recipient=user, type=NotificationType.SYSTEM).exists():
                n = notify(user, NotificationType.SYSTEM, "Welcome to Kakuma ResourceLink",
                           "Explore opportunities, read stories and connect with alumni.")
                if n:
                    Notification.objects.filter(pk=n.pk).update(is_read=True, read_at=timezone.now())

    def _report(self, password):
        self.stdout.write(self.style.SUCCESS(f"Test data ready. Every test account uses the password: {password}"))
        for user in User.objects.filter(TEST_EMAILS).order_by("role", "email"):
            self.stdout.write(f"  {user.role:<10} {user.email:<34} {user.get_preferred_language_display()}")
