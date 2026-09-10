package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminStudyGroupListResponse(
    List<PartGroupsResponse> byPart
) {

    public static AdminStudyGroupListResponse from(AdminStudyGroupListInfo info) {
        return AdminStudyGroupListResponse.builder()
            .byPart(info.byPart().stream().map(PartGroupsResponse::from).toList())
            .build();
    }

    @Builder
    public record PartGroupsResponse(
        ChallengerPart part,
        List<StudyGroupResponse> groups
    ) {

        public static PartGroupsResponse from(AdminStudyGroupListInfo.PartGroupsInfo info) {
            return PartGroupsResponse.builder()
                .part(info.part())
                .groups(info.groups().stream().map(StudyGroupResponse::from).toList())
                .build();
        }
    }

    @Builder
    public record StudyGroupResponse(
        Long studyGroupId,
        String name,
        long scheduleCount
    ) {

        public static StudyGroupResponse from(AdminStudyGroupListInfo.StudyGroupInfo info) {
            return StudyGroupResponse.builder()
                .studyGroupId(info.studyGroupId())
                .name(info.name())
                .scheduleCount(info.scheduleCount())
                .build();
        }
    }
}
