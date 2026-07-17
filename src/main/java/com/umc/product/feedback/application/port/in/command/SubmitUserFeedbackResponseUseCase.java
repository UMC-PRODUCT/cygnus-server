package com.umc.product.feedback.application.port.in.command;

import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.form.application.port.in.FormActorContext;

/**
 * 사용자 피드백 응답 제출 UseCase.
 * <p>
 * UserFeedbackTemplate에 연결된 Form 폼에 즉시 제출(SUBMITTED) 방식으로 응답을 저장합니다.
 * 동일 폼에 이미 응답한 경우 Form 도메인에서 예외를 발생시킵니다.
 *
 * @return 생성된 FormResponse ID
 */
public interface SubmitUserFeedbackResponseUseCase {

    Long submit(FormActorContext actorContext, SubmitUserFeedbackResponseCommand command);
}
