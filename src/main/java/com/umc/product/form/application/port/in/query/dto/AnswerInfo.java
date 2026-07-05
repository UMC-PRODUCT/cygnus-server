package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

/**
 * Answer 단건 조회 결과 DTO.
 * <p>
 * 객관식 응답의 경우 {@code selectedOptions} 에 AnswerChoice 펼쳐서 제공
 * - 각 선택지의 id / content(스냅샷) 포함. 선택지가 삭제된 경우 id=null 로 표현.
 * <p>
 * 주관식은 {@code textValue}, SCHEDULE 은 {@code times}, FILE 계열은 {@code fileIds} 로 제공.
 */
@Builder
public record AnswerInfo(
    Long id,
    Long formResponseId,
    Long questionId,
    QuestionType answeredAsType,
    String textValue,
    List<SelectedOption> selectedOptions,
    Set<String> fileIds,
    Set<Instant> times,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Answer + 해당 답변의 AnswerChoice 리스트로부터 DTO 조립.
     * 객관식이 아닌 답변의 경우 {@code choices} 는 빈 리스트.
     */
    public static AnswerInfo from(Answer answer, List<AnswerChoice> choices) {
        List<SelectedOption> selectedOptions = choices.stream()
            .map(SelectedOption::from)
            .toList();

        return AnswerInfo.builder()
            .id(answer.getId())
            .formResponseId(answer.getFormResponse().getId())
            .questionId(answer.getQuestion().getId())
            .answeredAsType(answer.getAnsweredAsType())
            .textValue(answer.getTextValue())
            .selectedOptions(selectedOptions)
            .fileIds(answer.getFileIds())
            .times(answer.getTimes())
            .createdAt(answer.getCreatedAt())
            .updatedAt(answer.getUpdatedAt())
            .build();
    }

    /**
     * AnswerChoice 를 조회용으로 펼친 뷰.
     * {@code questionOptionId} 는 원본 선택지 ID — 선택지가 삭제됐다면 null.
     * {@code answeredAsContent} 는 응답 시점의 선택지 내용 스냅샷.
     */
    @Builder
    public record SelectedOption(
        Long questionOptionId,
        String answeredAsContent
    ) {

        public static SelectedOption from(AnswerChoice choice) {
            QuestionOption option = choice.getQuestionOption();

            return SelectedOption.builder()
                .questionOptionId(option != null ? option.getId() : null)
                .answeredAsContent(choice.getAnsweredAsContent())
                .build();
        }
    }
}
