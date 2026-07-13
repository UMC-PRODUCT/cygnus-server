package com.umc.product.organization.application.port.in.query.dto.umcproduct;

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
    List<UmcProductPartMembershipInfo> partMemberships,
    List<UmcProductLeadershipInfo> productLeaderships,
    List<UmcProductSquadParticipationInfo> squadParticipations
) {
}
