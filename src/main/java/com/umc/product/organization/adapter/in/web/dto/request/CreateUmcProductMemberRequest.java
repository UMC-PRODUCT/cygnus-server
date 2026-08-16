package com.umc.product.organization.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductMemberRequest(
    @NotNull Long memberId,
    @Size(max = 2000) String introduction,
    String profileImageId,
    @NotEmpty List<@NotNull @Valid UmcProductActivityPeriodRequest> activityPeriods
) {
    public CreateUmcProductMemberCommand toCommand(Long requesterMemberId) {
        return CreateUmcProductMemberCommand.of(
            requesterMemberId,
            memberId,
            introduction,
            profileImageId,
            activityPeriods.stream().map(UmcProductActivityPeriodRequest::toCommand).toList()
        );
    }
}
