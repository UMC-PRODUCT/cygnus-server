package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;

public record UmcProductMemberResponse(
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
    List<UmcProductMemberActivityPeriodResponse> activityPeriods,
    List<UmcProductChapterMembershipResponse> chapterMemberships,
    List<UmcProductLeadershipResponse> productLeaderships,
    List<UmcProductSquadParticipationResponse> squadParticipations
) {
    public static UmcProductMemberResponse from(UmcProductMemberInfo info) {
        return new UmcProductMemberResponse(
            info.umcProductMemberId(),
            info.memberId(),
            info.memberName(),
            info.memberNickname(),
            info.memberSchoolName(),
            info.memberProfileImageId(),
            info.memberProfileImageUrl(),
            info.introduction(),
            info.umcProductProfileImageId(),
            info.umcProductProfileImageUrl(),
            info.activityPeriods().stream().map(UmcProductMemberActivityPeriodResponse::from).toList(),
            info.chapterMemberships().stream().map(UmcProductChapterMembershipResponse::from).toList(),
            info.productLeaderships().stream().map(UmcProductLeadershipResponse::from).toList(),
            info.squadParticipations().stream().map(UmcProductSquadParticipationResponse::from).toList()
        );
    }
}
