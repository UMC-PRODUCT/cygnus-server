package com.umc.product.inquiry.application.port.in.command.dto;

/**
 * 문의 담당 운영진 지정 명령. actorMemberId는 요청 주체(운영진 판정 대상), targetManagerId는 지정 대상 운영진이다.
 */
public record AssignInquiryManagerCommand(
    Long inquiryId,
    Long actorMemberId,
    Long targetManagerId
) {
}
