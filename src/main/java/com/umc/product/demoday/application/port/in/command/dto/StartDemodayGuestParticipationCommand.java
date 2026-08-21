package com.umc.product.demoday.application.port.in.command.dto;

/**
 * {@code requestId}는 응답과 Cookie가 유실되어도 같은 입장 시도를 재개하기 위한 선택 UUID다.
 * {@code existingEntryCodeId}는 기존 클라이언트의 Cookie 기반 재제출 호환성을 위해 사용한다.
 */
public record StartDemodayGuestParticipationCommand(
    Long pollId,
    String admissionCode,
    String requestId,
    Long existingEntryCodeId
) {
}
