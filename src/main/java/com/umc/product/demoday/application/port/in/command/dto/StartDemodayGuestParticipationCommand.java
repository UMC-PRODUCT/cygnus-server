package com.umc.product.demoday.application.port.in.command.dto;

/**
 * {@code existingEntryCodeId}는 요청에 이미 유효한 게스트 participant Cookie가 있을 때만 채워진다(nullable).
 * 같은 브라우저의 재제출 멱등 처리(계약: "이미 유효한 Cookie를 가진 같은 브라우저가 같은 코드를 다시
 * 제출하면 성공으로 처리")에 쓰인다. 이 값이 지금 제출된 코드의 entryCodeId와 같으면 re-redeem 하지 않는다.
 */
public record StartDemodayGuestParticipationCommand(
    Long pollId,
    String admissionCode,
    Long existingEntryCodeId
) {
}
