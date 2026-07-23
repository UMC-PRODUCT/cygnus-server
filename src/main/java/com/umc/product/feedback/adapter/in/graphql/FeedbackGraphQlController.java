package com.umc.product.feedback.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.feedback.adapter.in.graphql.dto.SubmitFeedbackGraphQlRequest;
import com.umc.product.feedback.adapter.in.graphql.dto.UserFeedbackTemplateGraphQlResponse;
import com.umc.product.feedback.application.port.in.command.SubmitUserFeedbackResponseUseCase;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateUseCase;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FeedbackGraphQlController {

    private final GetUserFeedbackTemplateUseCase getUserFeedbackTemplateUseCase;
    private final SubmitUserFeedbackResponseUseCase submitUserFeedbackResponseUseCase;

    @QueryMapping
    public UserFeedbackTemplateGraphQlResponse feedbackTemplate(
        @CurrentMember MemberPrincipal principal,
        @Argument UserFeedbackContext context
    ) {
        return getUserFeedbackTemplateUseCase.findTemplate(principal.getMemberId(), context)
            .map(UserFeedbackTemplateGraphQlResponse::from)
            .orElse(null);
    }

    @MutationMapping
    public FeedbackSubmission submitFeedback(
        @CurrentMember MemberPrincipal principal,
        @Argument SubmitFeedbackGraphQlRequest input
    ) {
        Long formResponseId = submitUserFeedbackResponseUseCase.submit(input.toCommand(principal.getMemberId()));
        return new FeedbackSubmission(formResponseId);
    }

    public record FeedbackSubmission(Long formResponseId) {
    }
}
