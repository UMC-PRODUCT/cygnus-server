package com.umc.product.inhouse.application.port.in.command.dto;

import java.util.List;

public record RegisterUmcProductMemberCommand(
    Long requesterMemberId,
    String name,
    String nickname,
    String englishNickname,
    Long schoolId,
    String introduction,
    String profileImageId,
    List<UmcProductActivityPeriodCommand> activityPeriods,
    List<RegisterUmcProductChapterMembershipCommand> chapterMemberships,
    List<RegisterUmcProductDepartmentParticipationCommand> departmentParticipations,
    List<RegisterUmcProductLeadershipCommand> productLeaderships
) {
    public RegisterUmcProductMemberCommand {
        activityPeriods = immutable(activityPeriods);
        chapterMemberships = immutable(chapterMemberships);
        departmentParticipations = immutable(departmentParticipations);
        productLeaderships = immutable(productLeaderships);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
