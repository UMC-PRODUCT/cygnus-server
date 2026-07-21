package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "평가용 지원서 상세")
public record RecruitingApplicationDetailResponse(
    RecruitingApplicationSummaryResponse application,
    Long formResponseId,
    List<AnswerResponse> answers
) {

    public static RecruitingApplicationDetailResponse from(RecruitingApplicationDetailInfo info) {
        return new RecruitingApplicationDetailResponse(
            RecruitingApplicationSummaryResponse.from(info.application()),
            info.formResponseId(),
            info.answers().stream().map(AnswerResponse::from).toList()
        );
    }

    public record AnswerResponse(
        Long questionId,
        QuestionType type,
        String textValue,
        List<SelectedOptionResponse> selectedOptions,
        Set<String> fileIds,
        Set<Instant> times
    ) {

        private static AnswerResponse from(AnswerInfo info) {
            return new AnswerResponse(
                info.questionId(),
                info.answeredAsType(),
                info.textValue(),
                info.selectedOptions().stream().map(SelectedOptionResponse::from).toList(),
                info.fileIds(),
                info.times()
            );
        }
    }

    public record SelectedOptionResponse(Long questionOptionId, String answeredAsContent) {

        private static SelectedOptionResponse from(AnswerInfo.SelectedOption info) {
            return new SelectedOptionResponse(info.questionOptionId(), info.answeredAsContent());
        }
    }
}
