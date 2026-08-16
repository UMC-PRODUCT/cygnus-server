package com.umc.product.project.adapter.in.web.dto.common;

import java.util.List;

import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionEntry;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

/**
 * 지원 문항 한 개. {@code questionId} 가 null 이면 신규 추가, 있으면 기존 수정/이동.
 * <p>
 * 옵션은 RADIO / CHECKBOX / DROPDOWN 타입에서만 의미가 있으며, 그 외 타입은 빈 리스트여야 한다
 * (Service 레벨에서 거부됨).
 */
@Builder
public record ApplicationQuestionItem(
    Long questionId,

    @NotNull(message = "질문 타입은 필수입니다") QuestionType type,

    @NotBlank(message = "질문 제목은 필수입니다") String title,

    String description,

    boolean isRequired,

    @PositiveOrZero(message = "orderNo는 0 이상이어야 합니다") long orderNo,

    @NotNull(message = "옵션 리스트는 null 일 수 없습니다 (빈 리스트는 허용)")
    @Valid List<ApplicationQuestionOptionItem> options
) {

    public ApplicationQuestionEntry toEntry() {
        return ApplicationQuestionEntry.builder()
            .questionId(questionId)
            .type(type)
            .title(title)
            .description(description)
            .isRequired(isRequired)
            .orderNo(orderNo)
            .options(options.stream().map(ApplicationQuestionOptionItem::toEntry).toList())
            .build();
    }

    public static ApplicationQuestionItem from(ApplicationFormInfo.QuestionInfo info) {
        return ApplicationQuestionItem.builder()
            .questionId(info.questionId())
            .type(info.type())
            .title(info.title())
            .description(info.description())
            .isRequired(info.isRequired())
            .orderNo(info.orderNo())
            .options(info.options().stream().map(ApplicationQuestionOptionItem::from).toList())
            .build();
    }
}
