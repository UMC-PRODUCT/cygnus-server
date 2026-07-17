package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ForkQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Question(질문) 관리 UseCase.
 * <p>
 * 질문은 FormSection 에 속하며, 타입별로 하위 {@code QuestionOption}을 가질 수 있다 (RADIO/CHECKBOX/DROPDOWN).
 * 발행된 폼의 질문 구조 변경은 응답 일관성을 해치므로 DRAFT 상태에서만 허용. TODO: 관련 로직 확정 시 수정
 */
public interface ManageQuestionUseCase {

    /**
     * 섹션에 질문을 추가한다.
     * orderNo는 Service가 해당 섹션의 마지막 질문 뒤에 자동 부여.
     *
     * @return 생성된 Question ID
     */
    Long createQuestion(FormOwnerReference expectedOwner, FormActorContext actorContext, CreateQuestionCommand command);

    /**
     * 질문의 속성(title / description / type / isRequired)을 업데이트한다.
     * type 변경이 기존 선택지/응답과 불일치를 만드는 경우 Service가 정리 또는 예외 처리.
     * 발행된 폼의 질문은 수정 불가 — FORM_NOT_DRAFT 예외. TODO: 관련 로직 확정 시 수정
     */
    void updateQuestion(FormOwnerReference expectedOwner, FormActorContext actorContext, UpdateQuestionCommand command);

    /**
     * 질문 삭제. 연관 QuestionOption/AnswerChoice 도 cascade 삭제.
     */
    void deleteQuestion(FormOwnerReference expectedOwner, FormActorContext actorContext, DeleteQuestionCommand command);

    /**
     * 섹션 내 질문들의 순서를 재배치한다.
     * 입력 리스트 순서대로 orderNo가 1부터 재부여된다.
     * 섹션에 속한 모든 질문 ID가 포함되어야 한다.
     */
    void reorderQuestions(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        ReorderQuestionsCommand command
    );

    /**
     * 질문을 비활성화한다 (isActive = false).
     * 차수 사이 폼 수정 시 질문을 폼에서 제거할 때 사용하며, 기존 응답자의 Answer를 보존하기 위해 물리 삭제 대신 비활성화만 수행한다.
     */
    void deactivateQuestion(FormOwnerReference expectedOwner, FormActorContext actorContext, Long questionId);

    /**
     * 기존 질문을 기반으로 새 버전을 생성한다 (Copy-on-Write).
     * 원본 질문의 속성을 복사하고 원본은 비활성화(isActive=false)한다.
     * 선택지는 caller가 요청 데이터를 기반으로 별도 생성한다.
     * 차수 사이 폼 수정 시 기존 응답자의 질문 내용을 보존하기 위해 사용한다.
     *
     * @return 새로 생성된 Question ID
     */
    Long forkQuestion(FormOwnerReference expectedOwner, FormActorContext actorContext, ForkQuestionCommand command);
}
