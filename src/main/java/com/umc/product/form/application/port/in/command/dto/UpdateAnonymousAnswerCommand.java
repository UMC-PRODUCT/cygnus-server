package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * 익명 DRAFT FormResponse 의 개별 답변을 전체 교체하는 Command.
 * <p>
 * 기존 답변 값(textValue / AnswerChoice / fileIds / times)을 모두 삭제 후 새 값으로 재구성.
 * <p>
 * {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, {@code answerId} 로 찾은 응답의
 * 저장된 hash 와 sha256 매칭. 익명 경계 유출 방지를 위해 rawKey null 을 제외한 모든 실패 케이스
 * (Answer 없음, DRAFT 상태 아님, 기명 draft, hash 불일치) 는 FORBIDDEN 으로 통일.
 * null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
 */
@Builder
public record UpdateAnonymousAnswerCommand(
    Long answerId,
    String responseAccessKey,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
