package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.CloseFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UnpublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;

/**
 * Form(폼) 생명주기 관리 UseCase.
 * <p>
 * 생성 흐름: {@link #createDraft} 로 DRAFT 상태로 시작 -> {@link #updateForm}으로 임시저장 업데이트 -> {@link #publishForm} 로 PUBLISHED 전환 시 응답 수집 가능.
 * <p>
 * 삭제는 {@link #deleteForm} — 연관된 FormSection / Question / QuestionOption / FormResponse / Answer 모두 cascade 삭제.
 * <p>
 * 권한 검증과 발행 상태 검증은 호출 측 (consumer 도메인) 책임. 본 UseCase 는 단순 동작만 보장한다.
 * 응답 무결성은 {@code AnswerChoice.answeredAsContent} 스냅샷이 보장.
 */
public interface ManageFormUseCase {

    /**
     * 폼을 DRAFT 상태로 최초 생성한다. 이 단계에선 응답을 받을 수 없다.
     *
     * @return 생성된 Form ID
     */
    Long createDraft(CreateDraftFormCommand command);

    /**
     * 폼 메타데이터(title/description/isAnonymous) 부분 업데이트.
     * null 인 필드는 기존 값 유지.
     * 발행된 폼도 수정 가능하도록 구현.
     * TODO: 관련 정책 확정 시 수정
     */
    void updateForm(UpdateFormCommand command);

    /**
     * 폼을 DRAFT -> PUBLISHED 전환한다. 이후부터 응답 수집 가능.
     * 이미 PUBLISHED인 폼을 다시 발행하면 FORM_ALREADY_PUBLISHED 예외.
     */
    void publishForm(PublishFormCommand command);

    /** 응답이 없는 PUBLISHED 폼을 DRAFT로 되돌린다. */
    void unpublishForm(UnpublishFormCommand command);

    /** PUBLISHED 폼의 응답 수집을 영구 종료한다. */
    void closeForm(CloseFormCommand command);

    /**
     * 폼과 연관 구조(섹션/질문/옵션/응답/답변) 전부 cascade 삭제.
     */
    void deleteForm(DeleteFormCommand command);
}
