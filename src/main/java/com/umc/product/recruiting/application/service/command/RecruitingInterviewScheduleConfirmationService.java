package com.umc.product.recruiting.application.service.command;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand.Assignment;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewScheduleConfirmationService
    implements ConfirmRecruitingInterviewSchedulesUseCase {

    private static final long SLOT_UNIT_SECONDS = 15 * 60L;

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingInterviewSessionPort loadSessionPort;
    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final SaveRecruitingInterviewSchedulePort saveSchedulePort;
    private final FindRecruitingScheduleOverlapPort findOverlapPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public void confirmAll(ConfirmRecruitingInterviewSchedulesCommand command) {
        validateCommand(command);
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );

        List<Long> sessionIds = command.assignments().stream()
            .map(Assignment::sessionId)
            .distinct()
            .sorted()
            .toList();
        Map<Long, RecruitingInterviewSession> sessions = indexSessions(
            sessionIds,
            loadSessionPort.getAllByIdsForUpdate(sessionIds)
        );

        List<Long> applicationIds = command.assignments().stream()
            .map(Assignment::applicationId)
            .sorted()
            .toList();
        Map<Long, RecruitingApplication> applications = concurrencyLockService.lockApplications(applicationIds).stream()
            .collect(Collectors.toMap(RecruitingApplication::getId, Function.identity()));
        Map<Long, RecruitingInterviewSchedule> schedules = loadSchedulePort
            .getAllByApplicationIdsForUpdate(applicationIds).stream()
            .collect(Collectors.toMap(
                schedule -> schedule.getApplication().getId(),
                Function.identity(),
                (first, ignored) -> first,
                LinkedHashMap::new
            ));

        validateLockedRows(command, round, sessions, applications, schedules);
        Map<Instant, Set<Long>> availableResponses = loadAvailability(round, schedules.values().stream()
            .map(RecruitingInterviewSchedule::getAvailabilityFormResponseId)
            .toList());
        validateAssignments(command.assignments(), sessions, schedules, availableResponses);

        List<RecruitingInterviewSchedule> confirmed = command.assignments().stream()
            .map(assignment -> confirm(assignment, sessions.get(assignment.sessionId()), schedules))
            .toList();
        saveSchedulePort.saveAllAndFlush(confirmed);
    }

    private void validateCommand(ConfirmRecruitingInterviewSchedulesCommand command) {
        if (command == null
            || command.roundId() == null
            || command.roundId() <= 0
            || command.requesterMemberId() == null
            || command.requesterMemberId() <= 0
            || command.assignments().isEmpty()
            || command.assignments().stream().anyMatch(this::hasInvalidRequiredValue)) {
            throw invalidAssignment();
        }
        Set<Long> applicationIds = new HashSet<>();
        Set<SessionSlot> slots = new HashSet<>();
        for (Assignment assignment : command.assignments()) {
            if (!applicationIds.add(assignment.applicationId())
                || !slots.add(new SessionSlot(assignment.sessionId(), assignment.startsAt()))) {
                throw assignmentConflict();
            }
        }
    }

    private boolean hasInvalidRequiredValue(Assignment assignment) {
        return assignment == null
            || assignment.applicationId() == null
            || assignment.applicationId() <= 0
            || assignment.sessionId() == null
            || assignment.sessionId() <= 0
            || assignment.startsAt() == null;
    }

    private Map<Long, RecruitingInterviewSession> indexSessions(
        List<Long> requestedIds,
        List<RecruitingInterviewSession> lockedSessions
    ) {
        Map<Long, RecruitingInterviewSession> sessions = lockedSessions.stream()
            .collect(Collectors.toMap(RecruitingInterviewSession::getId, Function.identity()));
        if (sessions.size() != requestedIds.size()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_NOT_FOUND);
        }
        return sessions;
    }

    private void validateLockedRows(
        ConfirmRecruitingInterviewSchedulesCommand command,
        RecruitingRound round,
        Map<Long, RecruitingInterviewSession> sessions,
        Map<Long, RecruitingApplication> applications,
        Map<Long, RecruitingInterviewSchedule> schedules
    ) {
        if (applications.size() != command.assignments().size()
            || schedules.size() != command.assignments().size()) {
            throw invalidAssignment();
        }
        for (Assignment assignment : command.assignments()) {
            RecruitingInterviewSession session = sessions.get(assignment.sessionId());
            RecruitingApplication application = applications.get(assignment.applicationId());
            RecruitingInterviewSchedule schedule = schedules.get(assignment.applicationId());
            if (!Objects.equals(session.getRoundId(), round.getId())
                || !Objects.equals(application.getRound().getId(), round.getId())
                || !Objects.equals(schedule.getApplication().getId(), application.getId())) {
                throw invalidAssignment();
            }
            if (application.getStatus() != RecruitingApplicationStatus.INTERVIEW_ASSIGNED
                || schedule.getStatus() != RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED
                || schedule.getAvailabilityFormResponseId() == null) {
                throw assignmentConflict();
            }
        }
    }

    private Map<Instant, Set<Long>> loadAvailability(RecruitingRound round, List<Long> responseIds) {
        Long formId = round.getAvailabilityFormId();
        Long questionId = round.getAvailabilityScheduleQuestionId();
        if (formId == null || formId <= 0 || questionId == null || questionId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);
        }
        Map<Instant, Set<Long>> result = new HashMap<>();
        for (RecruitingScheduleOverlapSlot overlap : findOverlapPort.findOverlaps(formId, questionId, responseIds)) {
            result.computeIfAbsent(overlap.startsAt(), ignored -> new HashSet<>())
                .addAll(overlap.availableFormResponseIds());
        }
        return result;
    }

    private void validateAssignments(
        List<Assignment> assignments,
        Map<Long, RecruitingInterviewSession> sessions,
        Map<Long, RecruitingInterviewSchedule> schedules,
        Map<Instant, Set<Long>> availableResponses
    ) {
        for (Assignment assignment : assignments) {
            RecruitingInterviewSession session = sessions.get(assignment.sessionId());
            RecruitingInterviewSchedule schedule = schedules.get(assignment.applicationId());
            Instant endsAt = derivedEndsAt(session, assignment.startsAt());
            validateSlot(session, assignment.startsAt(), endsAt);
            validateAvailability(
                schedule.getAvailabilityFormResponseId(),
                assignment.startsAt(),
                endsAt,
                availableResponses
            );
            if (loadSchedulePort.existsConfirmedBySessionIdAndStartsAt(
                assignment.sessionId(),
                assignment.startsAt()
            )) {
                throw assignmentConflict();
            }
            schedule.validateConfirmation(
                session.getId(),
                assignment.startsAt(),
                endsAt,
                session.getLocation(),
                assignment.contactSnapshot()
            );
        }
    }

    private void validateSlot(RecruitingInterviewSession session, Instant startsAt, Instant endsAt) {
        long offsetSeconds = Duration.between(session.getStartsAt(), startsAt).getSeconds();
        if (startsAt.getNano() != 0
            || Math.floorMod(startsAt.getEpochSecond(), SLOT_UNIT_SECONDS) != 0
            || startsAt.isBefore(session.getStartsAt())
            || endsAt.isAfter(session.getEndsAt())
            || Math.floorMod(offsetSeconds, session.getSlotDurationMinutes() * 60L) != 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID_SLOT);
        }
    }

    private void validateAvailability(
        Long responseId,
        Instant startsAt,
        Instant endsAt,
        Map<Instant, Set<Long>> availableResponses
    ) {
        for (Instant subSlot = startsAt; subSlot.isBefore(endsAt); subSlot = subSlot.plusSeconds(SLOT_UNIT_SECONDS)) {
            if (!availableResponses.getOrDefault(subSlot, Set.of()).contains(responseId)) {
                throw assignmentConflict();
            }
        }
    }

    private RecruitingInterviewSchedule confirm(
        Assignment assignment,
        RecruitingInterviewSession session,
        Map<Long, RecruitingInterviewSchedule> schedules
    ) {
        RecruitingInterviewSchedule schedule = schedules.get(assignment.applicationId());
        schedule.confirm(
            session.getId(),
            assignment.startsAt(),
            derivedEndsAt(session, assignment.startsAt()),
            session.getLocation(),
            assignment.contactSnapshot()
        );
        return schedule;
    }

    private Instant derivedEndsAt(RecruitingInterviewSession session, Instant startsAt) {
        return startsAt.plus(Duration.ofMinutes(session.getSlotDurationMinutes()));
    }

    private static RecruitingDomainException invalidAssignment() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);
    }

    private static RecruitingDomainException assignmentConflict() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT);
    }

    private record SessionSlot(Long sessionId, Instant startsAt) {
    }
}
