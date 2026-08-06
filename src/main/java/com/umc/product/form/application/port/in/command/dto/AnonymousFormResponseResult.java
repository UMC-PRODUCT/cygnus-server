package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 응답 생성/발급 결과.
 * <p>
 * {@code responseAccessKey} 는 raw 값 (Base64URL 인코딩된 32byte 랜덤 토큰).
 * 이후 익명 응답 조작(update / delete / submit) 시 이 값을 다시 서버에 전달해야 하며, 서버는 sha256 매칭으로 검증한다.
 * <p>
 * 소비 도메인은 이 raw 값을 <b>자체 서버 내부에만 보관</b>하는 것을 권장 — 클라이언트에 직접 노출 시 강한 엔트로피 이점이 훼손된다.
 */
@Builder
public record AnonymousFormResponseResult(
    Long formResponseId,
    String responseAccessKey
) {
}
