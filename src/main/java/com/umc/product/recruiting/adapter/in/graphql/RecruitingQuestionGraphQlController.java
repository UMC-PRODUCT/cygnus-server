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
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.ApplicationQuestion;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionGraphQlResponse.RoundQuestion;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewQuestionReplacementGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundManagementGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingInterviewQuestionsCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingQuestionGraphQlController {

    private final GetRecruitingInterviewQuestionUseCase getInterviewQuestionUseCase;
    private final ManageRecruitingRoundInterviewQuestionUseCase manageRoundQuestionUseCase;
    private final ManageRecruitingApplicationInterviewQuestionUseCase manageApplicationQuestionUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @BatchMapping(typeName = "RecruitingRoundManagement", field = "interviewQuestions")
    public Map<RecruitingRoundManagementGraphQlResponse, List<RoundQuestion>> roundQuestions(
        List<RecruitingRoundManagementGraphQlResponse> managements
    ) {
        Set<Long> roundIds = managements.stream()
            .map(RecruitingRoundManagementGraphQlResponse::roundId)
            .collect(Collectors.toSet());
        Map<Long, List<com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo>>
            byRoundId = getInterviewQuestionUseCase.listActiveRoundQuestionsByRoundIds(
                roundIds,
                permissionSupport.currentMemberId()
            );
        return managements.stream().collect(Collectors.toMap(
            Function.identity(),
            management -> byRoundId.getOrDefault(management.roundId(), List.of()).stream()
                .map(RoundQuestion::from)
                .toList(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @BatchMapping(typeName = "RecruitingApplicationReview", field = "interviewQuestions")
    public Map<RecruitingApplicationReviewGraphQlResponse, List<ApplicationQuestion>> applicationQuestions(
        List<RecruitingApplicationReviewGraphQlResponse> reviews
    ) {
        Set<Long> applicationIds = reviews.stream()
            .map(RecruitingApplicationReviewGraphQlResponse::applicationId)
            .collect(Collectors.toSet());
        Map<Long, List<com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo>>
            byApplicationId = getInterviewQuestionUseCase.listActiveApplicationQuestionsByApplicationIds(
                applicationIds,
                permissionSupport.currentMemberId()
            );
        return reviews.stream().collect(Collectors.toMap(
            Function.identity(),
            review -> byApplicationId.getOrDefault(review.applicationId(), List.of()).stream()
                .map(ApplicationQuestion::from)
                .toList(),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse replaceRecruitingRoundInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument List<RecruitingInterviewQuestionReplacementGraphQlRequest> questions
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageRoundQuestionUseCase.replaceRoundQuestions(new ReplaceRecruitingInterviewQuestionsCommand.Round(
            roundId,
            requesterMemberId,
            entries(questions)
        ));
        return RecruitingRoundGraphQlResponse.from(getResourceUseCase.getRound(roundId, requesterMemberId));
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse replaceRecruitingApplicationInterviewQuestions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument List<RecruitingInterviewQuestionReplacementGraphQlRequest> questions
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageApplicationQuestionUseCase.replaceApplicationQuestions(
            new ReplaceRecruitingInterviewQuestionsCommand.Application(
                applicationId,
                requesterMemberId,
                entries(questions)
            )
        );
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(applicationId, requesterMemberId)
        );
    }

    private List<ReplaceRecruitingInterviewQuestionsCommand.Entry> entries(
        List<RecruitingInterviewQuestionReplacementGraphQlRequest> questions
    ) {
        return questions.stream()
            .map(question -> new ReplaceRecruitingInterviewQuestionsCommand.Entry(
                question.id(),
                question.content(),
                question.orderNo()
            ))
            .toList();
    }
}
