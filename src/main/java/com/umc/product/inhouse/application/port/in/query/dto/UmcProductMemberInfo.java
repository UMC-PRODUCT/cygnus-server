package com.umc.product.inhouse.application.port.in.query.dto;

import java.util.List;

public record UmcProductMemberInfo(
    Long umcProductMemberId,
    Long memberId,
    String memberName,
    String memberNickname,
    String memberSchoolName,
    String memberProfileImageId,
    String memberProfileImageUrl,
    String introduction,
    String umcProductProfileImageId,
    String umcProductProfileImageUrl,
    List<UmcProductMemberActivityPeriodInfo> activityPeriods,
    List<UmcProductChapterMembershipInfo> chapterMemberships,
    List<UmcProductLeadershipInfo> productLeaderships,
    List<UmcProductDepartmentParticipationInfo> departmentParticipations
) {
}
