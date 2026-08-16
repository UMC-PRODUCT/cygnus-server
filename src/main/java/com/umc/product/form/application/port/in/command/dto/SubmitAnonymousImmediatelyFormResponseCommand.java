package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 익명 응답을 즉시 제출하는 Command. (draft 없이 바로 SUBMITTED 상태 생성)
 * Vote 같이 한 번에 제출하는 익명 플로우에서 사용.
 * <p>
 * (익명 전용) 요청자 식별자 없이 발급되며, 응답 조작 시 필요한 {@code responseAccessKey}(raw) 를 발급받는다.
 * 반환은 {@link AnonymousFormResponseResult} — 서버는 sha256(rawKey) 만 저장하고 raw 값은 반환 후 유지하지 않는다.
 * <p>
 * 익명 응답의 중복 응답 정책은 form 엔진에서 검사하지 않는다 — 소비 도메인이 IP rate limit / 이메일 유일성 등으로 방어.
 * <p>
 * 결과 status 가 SUBMITTED 이므로 제출 무결성을 위해 형식 검증 + 필수 답변 누락 검증을 모두 수행.
 */
@Builder
public record SubmitAnonymousImmediatelyFormResponseCommand(
    Long formId,
    List<AnswerCommand> answers
) {
}
