package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 응답을 로그인 사용자의 memberId 로 등록(익명 → 기명 전환) 하는 Command.
 * <p>
 * DRAFT / SUBMITTED 상태 모두 대상. {@code respondentMemberId} 와 {@code responseAccessKeyHash} 를
 * 같은 UPDATE 로 원자적으로 전환해 XOR CHECK 를 위반하는 중간 상태를 만들지 않는다.
 * <p>
 * {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버는 sha256 매칭으로 대상 응답을 검증한다.
 * <p>
 * {@code requesterMemberId} (로그인 세션) 와 {@code responseAccessKey} 모두 필수.
 * 매칭 실패는 FORBIDDEN, 이미 기명 응답은 ALREADY_CLAIMED. null 파라미터는 각각 REQUIRED 계열 예외.
 */
@Builder
public record ClaimAnonymousFormResponseCommand(
    Long formResponseId,
    String responseAccessKey,
    Long requesterMemberId
) {
}
