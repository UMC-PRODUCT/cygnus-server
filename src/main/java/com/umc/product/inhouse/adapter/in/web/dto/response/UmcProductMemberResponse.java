package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberInfo;

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
    List<UmcProductDepartmentParticipationResponse> departmentParticipations
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
            info.departmentParticipations().stream().map(UmcProductDepartmentParticipationResponse::from).toList()
        );
    }
}
