"""Mentorship request state machine.

    mentor: pending → accepted | declined ; accepted → completed
    mentee: pending → cancelled ; accepted → completed
"""
from django.db import IntegrityError, transaction
from django.utils import timezone
from django.utils.translation import gettext_lazy as _
from rest_framework.exceptions import PermissionDenied, ValidationError

from apps.common.exceptions import ConflictError, InvalidTransition
from apps.diaspora.models import AlumniProfile, Visibility
from apps.notifications.models import NotificationType
from apps.notifications.services import notify

from .models import MentorshipRequest, MentorshipStatus as S

MENTOR_TRANSITIONS = {S.PENDING: {S.ACCEPTED, S.DECLINED}, S.ACCEPTED: {S.COMPLETED}}
MENTEE_TRANSITIONS = {S.PENDING: {S.CANCELLED}, S.ACCEPTED: {S.COMPLETED}}


def create_request(*, mentee, mentor_profile: AlumniProfile, goal: str, help_areas: list, message: str):
    if mentor_profile.user_id == mentee.id:
        raise ValidationError({"mentor_id": [_("You cannot request mentorship from yourself.")]})
    if not mentor_profile.is_mentor or mentor_profile.visibility == Visibility.PRIVATE:
        raise ValidationError({"mentor_id": [_("This person is not accepting mentorship requests.")]})
    try:
        with transaction.atomic():
            req = MentorshipRequest.objects.create(
                mentee=mentee, mentor=mentor_profile.user, goal=goal, help_areas=help_areas, message=message
            )
    except IntegrityError:
        raise ConflictError(_("You already have a pending request with this mentor."))
    notify(
        mentor_profile.user, NotificationType.MENTORSHIP_REQUEST, _("New mentorship request"),
        _("%(name)s would like your mentorship: %(goal)s"),
        {"request_id": str(req.id)}, params={"name": mentee.full_name, "goal": goal},
    )
    return req


@transaction.atomic
def change_status(req: MentorshipRequest, user, new_status: str, response_message: str = "") -> MentorshipRequest:
    req = MentorshipRequest.objects.select_for_update().get(pk=req.pk)
    if user.id == req.mentor_id:
        allowed, other = MENTOR_TRANSITIONS, req.mentee
    elif user.id == req.mentee_id:
        allowed, other = MENTEE_TRANSITIONS, req.mentor
    else:
        raise PermissionDenied()
    if new_status not in allowed.get(req.status, set()):
        raise InvalidTransition()
    req.status = new_status
    if user.id == req.mentor_id and response_message:
        req.response_message = response_message
    if req.responded_at is None and new_status in {S.ACCEPTED, S.DECLINED}:
        req.responded_at = timezone.now()
    req.save()
    notify(
        other, NotificationType.MENTORSHIP_UPDATE, _("Mentorship request update"),
        _("Your mentorship request “%(goal)s” is now %(status)s."),
        {"request_id": str(req.id), "status": new_status},
        params={"goal": req.goal, "status": S(new_status).label},
    )
    return req
