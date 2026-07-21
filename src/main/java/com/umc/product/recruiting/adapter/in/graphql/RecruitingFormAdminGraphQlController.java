package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpsertRecruitingApplicationFormGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingFormAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @MutationMapping
    public RecruitingIdGraphQlResponse upsertRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument UpsertRecruitingApplicationFormGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(
            upsertFormUseCase.upsert(input.toCommand(seasonId, roundId, requesterMemberId))
        );
    }
}
