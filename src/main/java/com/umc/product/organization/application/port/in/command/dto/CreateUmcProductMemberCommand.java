package com.umc.product.organization.application.port.in.command.dto;

import java.util.List;

public record CreateUmcProductMemberCommand(
    Long requesterMemberId,
    Long memberId,
    String introduction,
    String profileImageId,
    List<UmcProductActivityPeriodCommand> activityPeriods
) {
    public static CreateUmcProductMemberCommand of(
        Long requesterMemberId,
        Long memberId,
        String introduction,
        String profileImageId,
        List<UmcProductActivityPeriodCommand> activityPeriods
    ) {
        return new CreateUmcProductMemberCommand(
            requesterMemberId,
            memberId,
            introduction,
            profileImageId,
            activityPeriods
        );
    }
}
