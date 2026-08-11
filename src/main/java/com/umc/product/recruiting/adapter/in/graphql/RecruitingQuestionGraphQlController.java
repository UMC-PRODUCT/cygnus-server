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
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.ApplicationCreate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.ApplicationDeactivate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.ApplicationUpdate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.RoundCreate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.RoundDeactivate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest.RoundUpdate;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.ApplicationQuestion;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.RoundQuestion;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionIdGraphQlPayload;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingQuestionGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingInterviewQuestionUseCase getInterviewQuestionUseCase;
    private final ManageRecruitingRoundInterviewQuestionUseCase manageRoundQuestionUseCase;
    private final ManageRecruitingApplicationInterviewQuestionUseCase manageApplicationQuestionUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RelayConnection<RoundQuestion> recruitingRoundInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String roundId,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long decodedSeasonId = GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(decodedRoundId, decodedSeasonId)
        );
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getInterviewQuestionUseCase.listActiveRoundQuestions(decodedRoundId, requesterMemberId),
            arguments,
            RoundQuestion::from
        );
    }

    @QueryMapping
    public RelayConnection<ApplicationQuestion> recruitingApplicationInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String applicationId,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long decodedSeasonId = GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        Long decodedApplicationId = GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(decodedApplicationId, decodedSeasonId)
        );
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getInterviewQuestionUseCase.listActiveApplicationQuestions(decodedApplicationId, requesterMemberId),
            arguments,
            ApplicationQuestion::from
        );
    }

    @MutationMapping
    public RecruitingInterviewQuestionIdGraphQlPayload createRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RoundCreate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingInterviewQuestionIdGraphQlPayload.of(
            manageRoundQuestionUseCase.createRoundQuestion(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RoundUpdate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundQuestionUseCase.updateRoundQuestion(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload deactivateRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RoundDeactivate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundQuestionUseCase.deactivateRoundQuestion(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public RecruitingInterviewQuestionIdGraphQlPayload createRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument ApplicationCreate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingInterviewQuestionIdGraphQlPayload.of(
            manageApplicationQuestionUseCase.createApplicationQuestion(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument ApplicationUpdate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageApplicationQuestionUseCase.updateApplicationQuestion(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload deactivateRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument ApplicationDeactivate input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageApplicationQuestionUseCase.deactivateApplicationQuestion(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }
}
