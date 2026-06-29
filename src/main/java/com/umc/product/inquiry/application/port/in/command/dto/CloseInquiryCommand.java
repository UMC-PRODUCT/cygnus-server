package com.umc.product.inquiry.application.port.in.command.dto;

/**
 * 문의 종료 명령. actorMemberId는 요청 주체(운영진 판정 대상)이다.
 */
public record CloseInquiryCommand(
    Long inquiryId,
    Long actorMemberId
) {
    public static CloseInquiryCommand of(Long inquiryId, Long actorMemberId) {
        return new CloseInquiryCommand(inquiryId, actorMemberId);
    }
}
