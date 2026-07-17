package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * FormSection(폼 섹션) 관리 UseCase.
 * <p>
 * 권한 검증과 발행 상태 검증은 호출 측 (consumer 도메인) 책임. 본 UseCase 는 단순 동작만 보장.
 * 응답 무결성은 {@code AnswerChoice.answeredAsContent} 스냅샷이 보장.
 * <p>
 * TODO: 발행된 폼 / 응답이 들어온 폼 의 섹션 구조 변경 차단 정책 합의 시 분기 추가.
 */
public interface ManageFormSectionUseCase {

    /**
     * 폼에 섹션을 추가한다.
     * orderNo는 Service가 해당 폼의 마지막 섹션 뒤에 자동 부여.
     *
     * @return 생성된 FormSection ID
     */
    Long createSection(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        CreateFormSectionCommand command
    );

    /**
     * 섹션의 title/description 부분 업데이트.
     * null 인 필드는 기존 값 유지.
     */
    void updateSection(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        UpdateFormSectionCommand command
    );

    /**
     * 섹션을 삭제한다. 연관 Question / QuestionOption 도 cascade 삭제.
     */
    void deleteSection(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        DeleteFormSectionCommand command
    );

    /**
     * 폼 내 섹션들의 순서를 재배치한다.
     * 입력 리스트 순서대로 orderNo가 1부터 재부여된다.
     * 폼의 모든 섹션 ID 가 누락 / 중복 / 외부 ID 없이 정확히 일치해야 한다.
     */
    void reorderSections(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        ReorderFormSectionsCommand command
    );
}
