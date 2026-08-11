package com.umc.product.inhouse.adapter.in.web.dto.request;

import com.umc.product.inhouse.application.port.in.command.dto.LinkUmcProductMemberAccountCommand;

import jakarta.validation.constraints.NotNull;

public record LinkUmcProductMemberAccountRequest(
    @NotNull Long memberId
) {
    public LinkUmcProductMemberAccountCommand toCommand(Long requesterMemberId, Long umcProductMemberId) {
        return new LinkUmcProductMemberAccountCommand(requesterMemberId, umcProductMemberId, memberId);
    }
}
