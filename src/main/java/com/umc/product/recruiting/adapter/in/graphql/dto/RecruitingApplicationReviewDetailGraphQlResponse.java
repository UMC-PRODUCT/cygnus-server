package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;

public record RecruitingApplicationReviewDetailGraphQlResponse(
    RecruitingApplicationReviewGraphQlResponse application,
    String formResponseId,
    List<Answer> answers
) {

    public static RecruitingApplicationReviewDetailGraphQlResponse from(RecruitingApplicationDetailInfo info) {
        return new RecruitingApplicationReviewDetailGraphQlResponse(
            RecruitingApplicationReviewGraphQlResponse.from(info.application()),
            GlobalId.encode(GlobalIdTypes.FORM_RESPONSE, info.formResponseId()),
            info.answers().stream().map(Answer::from).toList()
        );
    }

    public record Answer(
        String questionId,
        QuestionType type,
        String textValue,
        List<SelectedOption> selectedOptions,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        private static Answer from(AnswerInfo info) {
            return new Answer(
                GlobalId.encode(GlobalIdTypes.FORM_QUESTION, info.questionId()),
                info.answeredAsType(),
                info.textValue(),
                info.selectedOptions().stream().map(SelectedOption::from).toList(),
                info.fileIds(),
                info.times()
            );
        }
    }

    public record SelectedOption(String questionOptionId, String answeredAsContent) {

        private static SelectedOption from(AnswerInfo.SelectedOption info) {
            return new SelectedOption(
                info.questionOptionId() == null
                    ? null
                    : GlobalId.encode(GlobalIdTypes.FORM_OPTION, info.questionOptionId()),
                info.answeredAsContent()
            );
        }
    }
}
