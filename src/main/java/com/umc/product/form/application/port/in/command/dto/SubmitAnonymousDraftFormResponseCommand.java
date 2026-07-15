package com.umc.product.form.application.port.in.command.dto;

import java.util.Set;

import lombok.Builder;

/**
 * 익명 draft 응답을 SUBMITTED 상태로 전환(최종 제출)하는 Command.
 * <p>
 * 답변 내용은 이전 익명 updateDraft 로 저장된 값 그대로 유지 — 이 Command 는 상태 전이만 담당.
 * 최종 제출 직전에 답변을 한 번 더 바꾸려면 익명 updateDraft 를 먼저 호출한 뒤 submit.
 * <p>
 * draft 가 아닌 응답을 submit 시도하거나 매칭되는 draft 가 없으면 예외. {@code submittedIp} 는 감사/분석용 (선택, null 가능).
 * {@code requiredQuestionIds} / {@code allowedQuestionIds} 는 특정 제품 흐름에서 제출 검증 범위를 좁힐 때 사용한다.
 * 둘 다 {@code null} 이면 기존처럼 form 전체 기준으로 검증한다.
 * <p>
 * (익명 전용) {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버는 sha256 매칭으로 draft 를 찾는다.
 * 매칭 실패, DRAFT 상태 아님, 기명 draft 인 경우 모두 FORBIDDEN. null 이면 RESPONSE_ACCESS_KEY_REQUIRED 예외.
 */
@Builder
public record SubmitAnonymousDraftFormResponseCommand(
    String responseAccessKey,
    String submittedIp,
    Set<Long> requiredQuestionIds,
    Set<Long> allowedQuestionIds
) {
}
