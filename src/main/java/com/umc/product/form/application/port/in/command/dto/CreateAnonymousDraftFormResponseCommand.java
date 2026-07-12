package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 draft 응답을 최초 생성하는 Command.
 * <p>
 * (익명 전용) 요청자 식별자 없이 발급되며, 응답 조작 시 필요한 {@code responseAccessKey}(raw) 를 발급받는다.
 * 반환은 {@link AnonymousFormResponseResult} — 서버는 sha256(rawKey) 만 저장하고 raw 값은 반환 후 유지하지 않는다.
 * <p>
 * 익명 응답의 중복 응답 정책은 form 엔진에서 검사하지 않는다 — 소비 도메인이 IP rate limit / 이메일 유일성 등으로 방어.
 */
@Builder
public record CreateAnonymousDraftFormResponseCommand(
    Long formId
) {
}
