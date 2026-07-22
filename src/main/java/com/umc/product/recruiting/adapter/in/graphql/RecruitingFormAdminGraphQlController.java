package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpsertRecruitingApplicationFormGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingFormAdminGraphQlController {

    private final UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @MutationMapping
    public RecruitingRoundGraphQlResponse upsertRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument UpsertRecruitingApplicationFormGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingRoundGraphQlResponse round = RecruitingRoundGraphQlResponse.from(
            getResourceUseCase.getRound(roundId, requesterMemberId)
        );
        permissionSupport.assertRecruitmentPermission(
            requesterMemberId,
            round.seasonId(),
            PermissionType.WRITE
        );
        upsertFormUseCase.upsert(input.toCommand(round.seasonId(), roundId, requesterMemberId));
        return RecruitingRoundGraphQlResponse.from(getResourceUseCase.getRound(roundId, requesterMemberId));
    }
}
