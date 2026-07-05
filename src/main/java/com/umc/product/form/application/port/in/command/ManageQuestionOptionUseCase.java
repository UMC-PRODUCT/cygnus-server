package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;

/**
 * QuestionOption(질문 선택지) 관리 UseCase.
 * <p>
 * 선택지는 RADIO / CHECKBOX / DROPDOWN 타입 질문에만 유의미.
 * 타입 검증은 호출 측 책임.
 * 권한 검증과 발행 상태 검증은 호출 측 (consumer 도메인) 책임. 본 UseCase 는 단순 동작만 보장.
 * 응답 무결성은 {@code AnswerChoice.answeredAsContent} 스냅샷이 보장.
 * <p>
 * TODO: 발행된 폼 / 응답이 들어온 폼의 선택지 변경 차단 정책 합의 시 분기 추가.
 */
public interface ManageQuestionOptionUseCase {

    /**
     * 질문에 선택지를 추가한다.
     * orderNo는 Service가 해당 질문의 마지막 선택지 뒤에 자동 부여.
     *
     * @return 생성된 QuestionOption ID
     */
    Long createOption(CreateQuestionOptionCommand command);

    /**
     * 선택지의 content / isOther 부분 업데이트.
     * null 인 필드는 기존 값 유지.
     */
    void updateOption(UpdateQuestionOptionCommand command);

    /**
     * 선택지 삭제. 연관 AnswerChoice의 question_option_id는 ON DELETE SET NULL로 처리됨.
     */
    void deleteOption(DeleteQuestionOptionCommand command);

    /**
     * 질문 내 선택지들의 순서를 재배치한다.
     * 입력 리스트 순서대로 orderNo가 1부터 재부여된다.
     * 질문의 모든 선택지 ID 가 누락 / 중복 / 외부 ID 없이 정확히 일치해야 한다.
     */
    void reorderOptions(ReorderQuestionOptionsCommand command);
}
