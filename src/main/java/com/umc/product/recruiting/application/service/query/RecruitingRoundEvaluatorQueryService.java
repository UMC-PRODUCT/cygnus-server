package com.umc.product.recruiting.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingRoundEvaluatorQueryService implements GetRecruitingRoundEvaluatorUseCase {

    private final LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public List<RecruitingRoundEvaluatorInfo> listByRoundIdAndStage(
        Long roundId,
        RecruitingEvaluatorStage stage
    ) {
        return loadEvaluatorPort.listByRoundIdAndStage(roundId, stage).stream()
            .map(RecruitingRoundEvaluatorInfo::from)
            .toList();
    }

    @Override
    public List<RecruitingRoundEvaluatorInfo> listByRoundIdAndStage(
        Long roundId,
        RecruitingEvaluatorStage stage,
        Long requesterMemberId
    ) {
        authorizeManagementUseCase.authorizeSeasonManagement(
            requesterMemberId,
            loadRoundPort.getById(roundId).getSeason().getId()
        );
        return listByRoundIdAndStage(roundId, stage);
    }

    @Override
    public boolean canEvaluate(Long roundId, Long memberId, RecruitingEvaluatorStage stage) {
        return loadEvaluatorPort.existsByRoundIdAndMemberIdAndStage(roundId, memberId, stage);
    }
}
