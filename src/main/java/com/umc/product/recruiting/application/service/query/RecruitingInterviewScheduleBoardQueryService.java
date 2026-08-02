package com.umc.product.recruiting.application.service.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo.ApplicantInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo.SessionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo.SlotInfo;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewScheduleBoardPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleBoardRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewScheduleBoardQueryService implements GetRecruitingInterviewScheduleBoardUseCase {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final long SLOT_SECONDS = 15 * 60L;

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingInterviewSessionPort loadSessionPort;
    private final LoadRecruitingInterviewScheduleBoardPort loadBoardPort;
    private final FindRecruitingScheduleOverlapPort findOverlapPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public RecruitingInterviewScheduleBoardInfo getBoard(Long roundId, LocalDate date, Long requesterMemberId) {
        if (date == null) {
            throw invalidSchedule();
        }
        RecruitingRound round = loadRoundPort.getById(roundId);
        authorizeManagementUseCase.authorizeSeasonManagement(requesterMemberId, round.getSeason().getId());

        Instant dayStart = date.atStartOfDay(KOREA_ZONE).toInstant();
        Instant nextDayStart = date.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant();
        List<RecruitingInterviewSession> allSessions = loadSessionPort.listByRoundId(roundId);
        Map<Long, RecruitingInterviewSession> sessionsById = allSessions.stream()
            .collect(Collectors.toMap(RecruitingInterviewSession::getId, Function.identity()));
        List<RecruitingInterviewSession> sessions = allSessions.stream()
            .filter(session -> !session.getStartsAt().isBefore(dayStart))
            .filter(session -> session.getStartsAt().isBefore(nextDayStart))
            .toList();

        List<RecruitingInterviewScheduleBoardRow> rows = loadBoardPort.listByRoundId(roundId);
        List<RecruitingInterviewScheduleBoardRow> pendingRows = rows.stream()
            .filter(row -> row.status() == RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED)
            .toList();
        validateConfirmedRows(rows, sessionsById);

        Map<Long, Long> applicationIdByResponseId = pendingRows.stream()
            .filter(row -> row.availabilityFormResponseId() != null)
            .collect(Collectors.toMap(
                RecruitingInterviewScheduleBoardRow::availabilityFormResponseId,
                RecruitingInterviewScheduleBoardRow::applicationId,
                (first, ignored) -> first,
                LinkedHashMap::new
            ));
        Map<Instant, List<Long>> availableApplicationsByStart = loadAvailability(
            round,
            applicationIdByResponseId
        );

        Map<SessionSlot, ApplicantInfo> assignments = rows.stream()
            .filter(row -> row.status() == RecruitingInterviewScheduleStatus.CONFIRMED)
            .filter(row -> row.interviewSessionId() != null)
            .collect(Collectors.toMap(
                row -> new SessionSlot(row.interviewSessionId(), row.startsAt()),
                this::toApplicantInfo
            ));
        Set<Long> selectedSessionIds = sessions.stream()
            .map(RecruitingInterviewSession::getId)
            .collect(Collectors.toSet());

        List<SessionInfo> sessionInfos = sessions.stream()
            .map(session -> toSessionInfo(session, availableApplicationsByStart, assignments))
            .toList();
        List<ApplicantInfo> pendingApplicants = pendingRows.stream()
            .map(this::toApplicantInfo)
            .toList();
        List<ApplicantInfo> confirmedApplicants = rows.stream()
            .filter(row -> row.status() == RecruitingInterviewScheduleStatus.CONFIRMED)
            .filter(row -> selectedSessionIds.contains(row.interviewSessionId()))
            .map(this::toApplicantInfo)
            .toList();
        return new RecruitingInterviewScheduleBoardInfo(
            roundId,
            date,
            sessionInfos,
            pendingApplicants,
            confirmedApplicants
        );
    }

    private Map<Instant, List<Long>> loadAvailability(
        RecruitingRound round,
        Map<Long, Long> applicationIdByResponseId
    ) {
        if (applicationIdByResponseId.isEmpty()) {
            return Map.of();
        }
        Long formId = round.getAvailabilityFormId();
        Long questionId = round.getAvailabilityScheduleQuestionId();
        if (formId == null || formId <= 0 || questionId == null || questionId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);
        }
        List<Long> responseIds = List.copyOf(applicationIdByResponseId.keySet());
        List<RecruitingScheduleOverlapSlot> overlaps = findOverlapPort.findOverlaps(
            formId,
            questionId,
            responseIds
        );
        Map<Instant, List<Long>> result = new HashMap<>();
        for (RecruitingScheduleOverlapSlot overlap : overlaps) {
            List<Long> applicationIds = overlap.availableFormResponseIds().stream()
                .map(applicationIdByResponseId::get)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
            result.put(overlap.startsAt(), applicationIds);
        }
        return result;
    }

    private void validateConfirmedRows(
        List<RecruitingInterviewScheduleBoardRow> rows,
        Map<Long, RecruitingInterviewSession> sessionsById
    ) {
        rows.stream()
            .filter(row -> row.status() == RecruitingInterviewScheduleStatus.CONFIRMED)
            .filter(row -> row.interviewSessionId() != null)
            .forEach(row -> {
                RecruitingInterviewSession session = sessionsById.get(row.interviewSessionId());
                if (session == null || !isCalculatedSlot(session, row.startsAt())) {
                    throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID_SLOT);
                }
            });
    }

    private boolean isCalculatedSlot(RecruitingInterviewSession session, Instant startsAt) {
        if (startsAt == null || startsAt.isBefore(session.getStartsAt()) || !startsAt.isBefore(session.getEndsAt())) {
            return false;
        }
        return Math.floorMod(startsAt.getEpochSecond() - session.getStartsAt().getEpochSecond(), SLOT_SECONDS) == 0;
    }

    private SessionInfo toSessionInfo(
        RecruitingInterviewSession session,
        Map<Instant, List<Long>> availableApplicationsByStart,
        Map<SessionSlot, ApplicantInfo> assignments
    ) {
        List<SlotInfo> slots = new ArrayList<>();
        for (Instant startsAt = session.getStartsAt(); startsAt.isBefore(session.getEndsAt());
             startsAt = startsAt.plusSeconds(SLOT_SECONDS)) {
            slots.add(new SlotInfo(
                startsAt,
                startsAt.plusSeconds(SLOT_SECONDS),
                availableApplicationsByStart.getOrDefault(startsAt, List.of()),
                assignments.get(new SessionSlot(session.getId(), startsAt))
            ));
        }
        return new SessionInfo(
            session.getId(),
            session.getName(),
            session.getStartsAt(),
            session.getEndsAt(),
            session.getMode(),
            session.getLocation(),
            slots
        );
    }

    private ApplicantInfo toApplicantInfo(RecruitingInterviewScheduleBoardRow row) {
        return new ApplicantInfo(row.applicationId(), row.applicantName());
    }

    private static RecruitingDomainException invalidSchedule() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID);
    }

    private record SessionSlot(Long sessionId, Instant startsAt) {
    }
}
