package com.umc.product.inhouse.application.port.in.command.dto;

import java.util.List;

public record CreateUmcProductMemberCommand(
    Long requesterMemberId,
    String name,
    String nickname,
    Long schoolId,
    String introduction,
    String profileImageId,
    List<UmcProductActivityPeriodCommand> activityPeriods
) {
    public static CreateUmcProductMemberCommand of(
        Long requesterMemberId,
        String name,
        String nickname,
        Long schoolId,
        String introduction,
        String profileImageId,
        List<UmcProductActivityPeriodCommand> activityPeriods
    ) {
        return new CreateUmcProductMemberCommand(
            requesterMemberId,
            name,
            nickname,
            schoolId,
            introduction,
            profileImageId,
            activityPeriods
        );
    }
}
