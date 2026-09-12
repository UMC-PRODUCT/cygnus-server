package com.umc.product.inquiry.application.port.in.command.dto;

/**
 * 문의 담당 운영진 이관 명령. 기존 담당자(fromManagerId)를 제거하고 새 담당자(toManagerId)를 추가한다.
 */
public record TransferInquiryManagerCommand(
    Long inquiryId,
    Long actorMemberId,
    Long fromManagerId,
    Long toManagerId
) {
}
