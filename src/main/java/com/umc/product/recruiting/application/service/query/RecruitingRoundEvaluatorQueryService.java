package com.umc.product.recruiting.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingRoundEvaluatorQueryService implements GetRecruitingRoundEvaluatorUseCase {

    private final LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public List<RecruitingRoundEvaluatorInfo> listByRoundId(Long roundId) {
        return loadEvaluatorPort.listByRoundId(roundId).stream()
            .map(RecruitingRoundEvaluatorInfo::from)
            .toList();
    }

    @Override
    public Map<Long, List<RecruitingRoundEvaluatorInfo>> listByRoundIds(Set<Long> roundIds) {
        return loadEvaluatorPort.listByRoundIds(roundIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(RecruitingRoundEvaluatorInfo::from).toList()
            ));
    }

    @Override
    public List<RecruitingRoundEvaluatorInfo> listByRoundId(
        Long roundId,
        Long requesterMemberId
    ) {
        authorizeManagementUseCase.authorizeSeasonManagement(
            requesterMemberId,
            loadRoundPort.getById(roundId).getSeason().getId()
        );
        return listByRoundId(roundId);
    }

    @Override
    public boolean canEvaluate(Long roundId, Long memberId) {
        return loadEvaluatorPort.existsByRoundIdAndMemberId(roundId, memberId);
    }
}
