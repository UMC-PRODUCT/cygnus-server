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
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluationGraphQlController {

    private final GetRecruitingApplicationEvaluationUseCase getApplicationEvaluationUseCase;
    private final SubmitRecruitingApplicationEvaluationUseCase submitApplicationEvaluationUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @SchemaMapping(typeName = "RecruitingApplicationReview", field = "evaluations")
    public List<RecruitingApplicationEvaluationGraphQlResponse> evaluations(
        RecruitingApplicationReviewGraphQlResponse review,
        @Argument RecruitingEvaluatorStage stage
    ) {
        return getApplicationEvaluationUseCase.listVisibleEvaluations(
            review.applicationId(),
            permissionSupport.currentMemberId(),
            stage
        ).stream().map(RecruitingApplicationEvaluationGraphQlResponse::from).toList();
    }

    @BatchMapping(typeName = "RecruitingApplicationEvaluation", field = "evaluator")
    public Map<RecruitingApplicationEvaluationGraphQlResponse, MemberPublicInfo> evaluators(
        List<RecruitingApplicationEvaluationGraphQlResponse> evaluations
    ) {
        Set<Long> memberIds = evaluations.stream()
            .map(RecruitingApplicationEvaluationGraphQlResponse::evaluatorMemberId)
            .collect(Collectors.toSet());
        Map<Long, MemberPublicInfo> byId = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        return evaluations.stream().collect(Collectors.toMap(
            Function.identity(),
            evaluation -> byId.get(evaluation.evaluatorMemberId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitRecruitingApplicationEvaluation(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingApplicationEvaluationGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        submitApplicationEvaluationUseCase.submit(input.toSubmitCommand(applicationId, requesterMemberId));
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(applicationId, requesterMemberId)
        );
    }
}
