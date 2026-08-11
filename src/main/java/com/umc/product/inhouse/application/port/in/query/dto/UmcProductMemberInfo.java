package com.umc.product.inhouse.application.port.in.query.dto;

import java.util.List;

public record UmcProductMemberInfo(
    Long umcProductMemberId,
    String name,
    String nickname,
    Long schoolId,
    String schoolName,
    String introduction,
    String umcProductProfileImageId,
    String umcProductProfileImageUrl,
    List<UmcProductMemberActivityPeriodInfo> activityPeriods,
    List<UmcProductChapterMembershipInfo> chapterMemberships,
    List<UmcProductLeadershipInfo> productLeaderships,
    List<UmcProductDepartmentParticipationInfo> departmentParticipations
) {
}
