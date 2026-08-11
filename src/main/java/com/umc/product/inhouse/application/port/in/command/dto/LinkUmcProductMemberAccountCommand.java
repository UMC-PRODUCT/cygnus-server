package com.umc.product.inhouse.application.port.in.command.dto;

public record LinkUmcProductMemberAccountCommand(
    Long requesterMemberId,
    Long umcProductMemberId,
    Long memberId
) {
}
