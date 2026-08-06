package com.umc.product.recruiting.application.service.query;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewSessionInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewSessionQueryService implements GetRecruitingInterviewSessionUseCase {

    private final LoadRecruitingInterviewSessionPort loadSessionPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public RecruitingInterviewSessionInfo getSession(Long roundId, Long sessionId, Long requesterMemberId) {
        authorize(roundId, requesterMemberId);
        RecruitingInterviewSession session = loadSessionPort.getById(sessionId);
        if (!Objects.equals(session.getRoundId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_NOT_FOUND);
        }
        return RecruitingInterviewSessionInfo.from(session);
    }

    @Override
    public List<RecruitingInterviewSessionInfo> listSessions(Long roundId, Long requesterMemberId) {
        authorize(roundId, requesterMemberId);
        return loadSessionPort.listByRoundId(roundId).stream()
            .map(RecruitingInterviewSessionInfo::from)
            .toList();
    }

    private void authorize(Long roundId, Long requesterMemberId) {
        RecruitingRound round = loadRoundPort.getById(roundId);
        authorizeManagementUseCase.authorizeSeasonManagement(requesterMemberId, round.getSeason().getId());
    }
}
