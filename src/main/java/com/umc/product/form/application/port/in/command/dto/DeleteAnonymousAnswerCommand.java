package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 DRAFT FormResponse 의 개별 답변 삭제 Command. 연관 AnswerChoice 도 cascade 삭제.
 * <p>
 * response credential은 별도 actor context로 전달되며, {@code answerId} 로 찾은 응답의
 * 저장된 hash와 sha256 매칭한다. 익명 경계 유출 방지를 위해 credential 부재를 제외한 모든 실패 케이스
 * (Answer 없음, DRAFT 상태 아님, 기명 draft, hash 불일치) 는 FORBIDDEN 으로 통일.
 * actor credential이 없으면 RESPONSE_ACCESS_KEY_REQUIRED.
 */
@Builder
public record DeleteAnonymousAnswerCommand(
    Long answerId
) {
}
