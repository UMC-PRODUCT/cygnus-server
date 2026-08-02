package com.umc.product.recruiting.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.out.CheckRecruitingInterviewSessionReferencePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSessionPort;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewSessionCommandService implements ManageRecruitingInterviewSessionUseCase {

    private static final int SLOT_DURATION_MINUTES = 15;

    private final LoadRecruitingInterviewSessionPort loadSessionPort;
    private final SaveRecruitingInterviewSessionPort saveSessionPort;
    private final CheckRecruitingInterviewSessionReferencePort checkReferencePort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public Long createSession(CreateRecruitingInterviewSessionCommand command) {
        RecruitingRound round = lockAndAuthorize(command.roundId(), command.requesterMemberId());
        RecruitingInterviewSession session = RecruitingInterviewSession.create(
            round.getId(),
            command.name(),
            command.startsAt(),
            command.endsAt(),
            SLOT_DURATION_MINUTES,
            command.mode(),
            command.location(),
            round.getInterviewStartAt(),
            round.getInterviewEndAt()
        );
        return saveSessionPort.save(session).getId();
    }

    @Override
    public void updateSession(UpdateRecruitingInterviewSessionCommand command) {
        RecruitingRound round = lockAndAuthorize(command.roundId(), command.requesterMemberId());
        RecruitingInterviewSession session = getSessionInRound(command.sessionId(), round.getId());
        if (checkReferencePort.existsConfirmedBySessionId(session.getId())) {
            throw confirmedScheduleExists();
        }
        session.update(
            command.name(),
            command.startsAt(),
            command.endsAt(),
            SLOT_DURATION_MINUTES,
            command.mode(),
            command.location(),
            round.getInterviewStartAt(),
            round.getInterviewEndAt()
        );
        saveSessionPort.save(session);
    }

    @Override
    public void deleteSession(DeleteRecruitingInterviewSessionCommand command) {
        RecruitingRound round = lockAndAuthorize(command.roundId(), command.requesterMemberId());
        RecruitingInterviewSession session = getSessionInRound(command.sessionId(), round.getId());
        if (checkReferencePort.existsBySessionId(session.getId())) {
            throw confirmedScheduleExists();
        }
        saveSessionPort.delete(session);
    }

    private RecruitingRound lockAndAuthorize(Long roundId, Long requesterMemberId) {
        RecruitingRound round = concurrencyLockService.lockRound(roundId);
        authorizeManagementUseCase.authorizeSeasonManagement(requesterMemberId, round.getSeason().getId());
        return round;
    }

    private RecruitingInterviewSession getSessionInRound(Long sessionId, Long roundId) {
        RecruitingInterviewSession session = loadSessionPort.getById(sessionId);
        if (!Objects.equals(session.getRoundId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_NOT_FOUND);
        }
        return session;
    }

    private RecruitingDomainException confirmedScheduleExists() {
        return new RecruitingDomainException(
            RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_CONFIRMED_SCHEDULE_EXISTS
        );
    }
}
