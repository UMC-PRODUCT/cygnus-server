package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;

import lombok.Builder;

/**
 * FormResponse 메타 + 모든 Answer 를 합성한 facade DTO.
 * <p>
 * 응답 상세 화면 (응답자가 자기 응답 다시 보기 / 폼 작성자가 개별 응답 확인) 용도로
 * 한 번의 호출로 메타 + 답변 트리를 받기 위한 합성 정보. N+1 회피 책임은 Service 가 짐.
 */
@Builder
public record FormResponseWithAnswersInfo(
    Long id,
    Long formId,
    Long respondentMemberId,
    FormResponseStatus status,
    Instant submittedAt,
    String submittedIp,
    Instant lastSavedAt,
    Instant createdAt,
    Instant updatedAt,
    List<AnswerInfo> answers
) {

    public static FormResponseWithAnswersInfo from(FormResponse formResponse, List<AnswerInfo> answers) {
        return FormResponseWithAnswersInfo.builder()
            .id(formResponse.getId())
            .formId(formResponse.getForm().getId())
            .respondentMemberId(formResponse.getRespondentMemberId())
            .status(formResponse.getStatus())
            .submittedAt(formResponse.getSubmittedAt())
            .submittedIp(formResponse.getSubmittedIp())
            .lastSavedAt(formResponse.getLastSavedAt())
            .createdAt(formResponse.getCreatedAt())
            .updatedAt(formResponse.getUpdatedAt())
            .answers(answers)
            .build();
    }
}
