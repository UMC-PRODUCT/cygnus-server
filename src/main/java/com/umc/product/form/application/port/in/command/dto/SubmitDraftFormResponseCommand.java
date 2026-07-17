package com.umc.product.form.application.port.in.command.dto;

import java.util.Set;

import lombok.Builder;

/**
 * draft 응답을 SUBMITTED 상태로 전환(최종 제출)하는 Command.
 * <p>
 * 답변 내용은 이전 {@code updateDraft} 로 저장된 값 그대로 유지된다 — 이 Command 는 상태 전이만 담당.
 * 최종 제출 직전에 답변을 한 번 더 바꾸려면 {@code updateDraft} 를 먼저 호출한 뒤 submit.
 * <p>
 * draft 가 아닌 응답을 submit 시도하면 예외. {@code submittedIp} 는 감사/분석용 (선택, null 가능).
 * {@code requiredQuestionIds} / {@code allowedQuestionIds} 는 특정 제품 흐름에서 제출 검증 범위를 좁힐 때 사용한다.
 * 둘 다 {@code null} 이면 기존처럼 form 전체 기준으로 검증한다.
 * <p>
 * 기명 actor는 별도 context로 전달되며 draft 소유자 본인만 가능하다.
 * 소유자와 다르거나 draft가 익명이거나 인증 actor가 없으면 FORM_RESPONSE_FORBIDDEN 예외.
 */
@Builder
public record SubmitDraftFormResponseCommand(
    Long formResponseId,
    String submittedIp,
    Set<Long> requiredQuestionIds,
    Set<Long> allowedQuestionIds
) {
}
