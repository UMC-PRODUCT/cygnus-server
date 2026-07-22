package com.umc.product.recruiting.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundManagementGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SetRecruitingRoundEvaluatorsCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluatorAdminGraphQlController {

    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final ManageRecruitingRoundEvaluatorUseCase manageRoundEvaluatorUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @BatchMapping(typeName = "RecruitingRoundManagement", field = "evaluators")
    public Map<RecruitingRoundManagementGraphQlResponse, List<MemberPublicInfo>> evaluators(
        List<RecruitingRoundManagementGraphQlResponse> managements
    ) {
        Set<Long> roundIds = managements.stream()
            .map(RecruitingRoundManagementGraphQlResponse::roundId)
            .collect(Collectors.toSet());
        Map<Long, List<com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo>>
            evaluatorsByRoundId = getRoundEvaluatorUseCase.listByRoundIds(roundIds);
        Set<Long> memberIds = evaluatorsByRoundId.values().stream()
            .flatMap(List::stream)
            .map(com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo::memberId)
            .collect(Collectors.toSet());
        Map<Long, MemberPublicInfo> memberById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        return managements.stream().collect(Collectors.toMap(
            Function.identity(),
            management -> evaluatorsByRoundId.getOrDefault(management.roundId(), List.of()).stream()
                .map(evaluator -> memberById.get(evaluator.memberId()))
                .filter(java.util.Objects::nonNull)
                .toList(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse setRecruitingRoundEvaluators(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument List<Long> memberIds
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundEvaluatorUseCase.setEvaluators(new SetRecruitingRoundEvaluatorsCommand(
            roundId,
            requesterMemberId,
            Set.copyOf(memberIds)
        ));
        return RecruitingRoundGraphQlResponse.from(getResourceUseCase.getRound(roundId, requesterMemberId));
    }
}
