package com.umc.product.inhouse.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductMemberRequest(
    @NotBlank @Size(max = 30) String name,
    @NotBlank @Size(max = 20) String nickname,
    Long schoolId,
    @Size(max = 2000) String introduction,
    String profileImageId,
    @NotEmpty List<@NotNull @Valid UmcProductActivityPeriodRequest> activityPeriods
) {
    public CreateUmcProductMemberCommand toCommand(Long requesterMemberId) {
        return CreateUmcProductMemberCommand.of(
            requesterMemberId,
            name,
            nickname,
            schoolId,
            introduction,
            profileImageId,
            activityPeriods.stream().map(UmcProductActivityPeriodRequest::toCommand).toList()
        );
    }
}
