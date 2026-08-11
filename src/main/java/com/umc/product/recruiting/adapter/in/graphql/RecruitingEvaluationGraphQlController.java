package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.GraphQlSuccessPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationEvaluationGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluationGraphQlController {

    private final GetRecruitingApplicationEvaluationUseCase getApplicationEvaluationUseCase;
    private final SubmitRecruitingApplicationEvaluationUseCase submitApplicationEvaluationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RelayConnection<RecruitingApplicationEvaluationGraphQlResponse> recruitingApplicationEvaluations(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String applicationId,
        @Argument RecruitingEvaluatorStage stage,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getApplicationEvaluationUseCase.listVisibleEvaluations(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId,
                stage
            ),
            arguments,
            RecruitingApplicationEvaluationGraphQlResponse::from
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload submitRecruitingApplicationEvaluation(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationEvaluationGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        submitApplicationEvaluationUseCase.submit(input.toSubmitCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }
}
