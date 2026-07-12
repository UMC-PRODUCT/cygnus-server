package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluationGraphQlController {

    private final GetRecruitingApplicationEvaluationUseCase getApplicationEvaluationUseCase;
    private final SaveRecruitingApplicationEvaluationUseCase saveApplicationEvaluationUseCase;
    private final SubmitRecruitingApplicationEvaluationUseCase submitApplicationEvaluationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingApplicationEvaluationGraphQlResponse> recruitingApplicationEvaluations(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingEvaluatorStage stage
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return getApplicationEvaluationUseCase.listVisibleEvaluations(applicationId, requesterMemberId, stage)
            .stream()
            .map(RecruitingApplicationEvaluationGraphQlResponse::from)
            .toList();
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse saveRecruitingApplicationEvaluation(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingApplicationEvaluationGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingIdGraphQlResponse.from(
            saveApplicationEvaluationUseCase.saveDraft(input.toSaveCommand(applicationId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean submitRecruitingApplicationEvaluation(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingApplicationEvaluationGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        submitApplicationEvaluationUseCase.submit(input.toSubmitCommand(applicationId, requesterMemberId));
        return true;
    }
}
