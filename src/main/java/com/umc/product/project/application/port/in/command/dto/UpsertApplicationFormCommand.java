package com.umc.product.project.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.form.domain.enums.QuestionType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;

/**
 * 지원 폼 upsert Command (PROJECT-106).
 * <p>
 * 본문이 곧 폼의 새 상태가 된다 (PUT 시멘틱 — full replace).
 * <ul>
 *   <li>{@code sectionId == null} → 신규 섹션 추가</li>
 *   <li>본문에 있고 기존에도 있음 → 수정 / 순서 갱신</li>
 *   <li>본문에 없고 기존에는 있음 → 삭제 (cascade)</li>
 * </ul>
 * 질문/옵션도 동일 패턴 (각 entry 의 ID 기준 diff).
 * <p>
 * 섹션 타입 검증:
 * <ul>
 *   <li>{@code type == PART} 인데 {@code allowedParts} 가 비어있으면 도메인 예외</li>
 *   <li>{@code type == COMMON} 의 {@code allowedParts} 는 무시되고 빈 리스트로 저장</li>
 * </ul>
 */
@Builder
public record UpsertApplicationFormCommand(
    Long projectId,
    Long requesterMemberId,
    String title,
    String description,
    List<ApplicationFormSectionEntry> sections
) {

    public UpsertApplicationFormCommand {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(sections, "sections must not be null");
    }

    /**
     * 섹션 entry. {@code sectionId} 가 null 이면 신규 추가.
     */
    @Builder
    public record ApplicationFormSectionEntry(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ApplicationQuestionEntry> questions
    ) {
        public ApplicationFormSectionEntry {
            Objects.requireNonNull(type, "section type must not be null");
            Objects.requireNonNull(title, "section title must not be null");
            Objects.requireNonNull(questions, "section questions must not be null");
        }
    }

    /**
     * 질문 entry. {@code questionId} 가 null 이면 신규 추가.
     * 옵션은 RADIO / CHECKBOX / DROPDOWN 타입에서만 의미가 있다.
     */
    @Builder
    public record ApplicationQuestionEntry(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean isRequired,
        long orderNo,
        List<ApplicationQuestionOptionEntry> options
    ) {
        public ApplicationQuestionEntry {
            Objects.requireNonNull(type, "question type must not be null");
            Objects.requireNonNull(title, "question title must not be null");
            Objects.requireNonNull(options, "question options must not be null");
        }
    }

    /**
     * 옵션 entry. {@code optionId} 가 null 이면 신규 추가.
     */
    @Builder
    public record ApplicationQuestionOptionEntry(
        Long optionId,
        String content,
        long orderNo,
        boolean isOther
    ) {
        public ApplicationQuestionOptionEntry {
            Objects.requireNonNull(content, "option content must not be null");
        }
    }
}
