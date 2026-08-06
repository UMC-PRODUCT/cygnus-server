package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 DRAFT FormResponse 의 개별 답변 삭제 Command. 연관 AnswerChoice 도 cascade 삭제.
 * <p>
 * {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, {@code answerId} 로 찾은 응답의
 * 저장된 hash 와 sha256 매칭. 익명 경계 유출 방지를 위해 rawKey null 을 제외한 모든 실패 케이스
 * (Answer 없음, DRAFT 상태 아님, 기명 draft, hash 불일치) 는 FORBIDDEN 으로 통일.
 * null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
 */
@Builder
public record DeleteAnonymousAnswerCommand(
    Long answerId,
    String responseAccessKey
) {
}
