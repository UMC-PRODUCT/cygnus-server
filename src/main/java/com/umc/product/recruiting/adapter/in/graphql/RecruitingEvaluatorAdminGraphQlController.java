package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.GraphQlSuccessPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundEvaluatorGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundEvaluatorGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundEvaluatorIdGraphQlPayload;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingEvaluatorAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final ManageRecruitingRoundEvaluatorUseCase manageRoundEvaluatorUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RelayConnection<RecruitingRoundEvaluatorGraphQlResponse> recruitingRoundEvaluators(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String roundId,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long decodedSeasonId = GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        Long requesterMemberId = requireRound(memberPrincipal, decodedSeasonId, decodedRoundId);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, decodedSeasonId, PermissionType.READ);
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getRoundEvaluatorUseCase.listByRoundId(decodedRoundId),
            arguments,
            RecruitingRoundEvaluatorGraphQlResponse::from
        );
    }

    @MutationMapping
    public RecruitingRoundEvaluatorIdGraphQlPayload addRecruitingRoundEvaluator(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRoundEvaluatorGraphQlRequest input
    ) {
        Long requesterMemberId = requireRound(memberPrincipal, input.decodedSeasonId(), input.decodedRoundId());
        return RecruitingRoundEvaluatorIdGraphQlPayload.of(
            manageRoundEvaluatorUseCase.addEvaluator(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload removeRecruitingRoundEvaluator(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRoundEvaluatorGraphQlRequest input
    ) {
        Long requesterMemberId = requireRound(memberPrincipal, input.decodedSeasonId(), input.decodedRoundId());
        manageRoundEvaluatorUseCase.removeEvaluator(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    private Long requireRound(MemberPrincipal memberPrincipal, Long seasonId, Long roundId) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        return requesterMemberId;
    }
}
