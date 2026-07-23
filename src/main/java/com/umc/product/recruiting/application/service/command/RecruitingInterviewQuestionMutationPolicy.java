package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewQuestionMutationPolicy {

    private final LoadRecruitingSubmittedInterviewEvaluationPort loadSubmittedEvaluationPort;

    public void assertMutable(RecruitingRoundInterviewQuestion question) {
        question.assertEditableBeforeFirstEvaluationSubmission(
            loadSubmittedEvaluationPort.existsSubmittedByRoundId(question.getRound().getId())
        );
    }

    public void assertMutable(RecruitingApplicationInterviewQuestion question) {
        question.assertEditableBeforeFirstEvaluationSubmission(
            loadSubmittedEvaluationPort.existsSubmittedByApplicationId(question.getApplication().getId())
        );
    }
}
