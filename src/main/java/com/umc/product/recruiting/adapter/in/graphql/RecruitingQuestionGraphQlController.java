package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.ApplicationQuestion;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.RoundQuestion;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;
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
    public List<RoundQuestion> recruitingRoundInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        return getInterviewQuestionUseCase.listActiveRoundQuestions(roundId, requesterMemberId).stream()
            .map(RoundQuestion::from)
            .toList();
    }

    @QueryMapping
    public List<ApplicationQuestion> recruitingApplicationInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        return getInterviewQuestionUseCase.listActiveApplicationQuestions(applicationId, requesterMemberId).stream()
            .map(ApplicationQuestion::from)
            .toList();
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument RecruitingInterviewQuestionGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingIdGraphQlResponse.from(manageRoundQuestionUseCase.createRoundQuestion(
            CreateRecruitingRoundInterviewQuestionCommand.of(
                roundId,
                requesterMemberId,
                input.content(),
                input.orderNo()
            )
        ));
    }

    @MutationMapping
    public Boolean updateRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument Long questionId,
        @Argument RecruitingInterviewQuestionGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundQuestionUseCase.updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand.of(
            questionId,
            roundId,
            requesterMemberId,
            input.content(),
            input.orderNo()
        ));
        return true;
    }

    @MutationMapping
    public Boolean deactivateRecruitingRoundInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument Long questionId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundQuestionUseCase.deactivateRoundQuestion(
            DeactivateRecruitingRoundInterviewQuestionCommand.of(questionId, roundId, requesterMemberId)
        );
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingInterviewQuestionGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingIdGraphQlResponse.from(manageApplicationQuestionUseCase.createApplicationQuestion(
            CreateRecruitingApplicationInterviewQuestionCommand.of(
                applicationId,
                requesterMemberId,
                input.content(),
                input.orderNo()
            )
        ));
    }

    @MutationMapping
    public Boolean updateRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument Long questionId,
        @Argument RecruitingInterviewQuestionGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageApplicationQuestionUseCase.updateApplicationQuestion(
            UpdateRecruitingApplicationInterviewQuestionCommand.of(
                questionId,
                applicationId,
                requesterMemberId,
                input.content(),
                input.orderNo()
            )
        );
        return true;
    }

    @MutationMapping
    public Boolean deactivateRecruitingApplicationInterviewQuestion(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument Long questionId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageApplicationQuestionUseCase.deactivateApplicationQuestion(
            DeactivateRecruitingApplicationInterviewQuestionCommand.of(
                questionId,
                applicationId,
                requesterMemberId
            )
        );
        return true;
    }
}
