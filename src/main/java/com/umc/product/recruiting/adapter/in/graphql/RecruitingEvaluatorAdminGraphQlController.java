package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundEvaluatorGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundEvaluatorGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluatorAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final ManageRecruitingRoundEvaluatorUseCase manageRoundEvaluatorUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingRoundEvaluatorGraphQlResponse> recruitingRoundEvaluators(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument RecruitingEvaluatorStage stage
    ) {
        Long requesterMemberId = requireRound(memberPrincipal, seasonId, roundId);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.READ);
        return getRoundEvaluatorUseCase.listByRoundIdAndStage(roundId, stage).stream()
            .map(RecruitingRoundEvaluatorGraphQlResponse::from)
            .toList();
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse addRecruitingRoundEvaluator(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument RecruitingRoundEvaluatorGraphQlRequest input
    ) {
        Long requesterMemberId = requireRound(memberPrincipal, seasonId, roundId);
        return RecruitingIdGraphQlResponse.from(
            manageRoundEvaluatorUseCase.addEvaluator(input.toCommand(roundId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean removeRecruitingRoundEvaluator(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument RecruitingRoundEvaluatorGraphQlRequest input
    ) {
        Long requesterMemberId = requireRound(memberPrincipal, seasonId, roundId);
        manageRoundEvaluatorUseCase.removeEvaluator(input.toCommand(roundId, requesterMemberId));
        return true;
    }

    private Long requireRound(MemberPrincipal memberPrincipal, Long seasonId, Long roundId) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        return requesterMemberId;
    }
}
