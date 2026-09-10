package com.umc.product.analytics.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminStudyGroupActivityResponse(
    long totalGroupCount,
    long totalScheduleCount,
    List<PartActivityResponse> byPart
) {

    public static AdminStudyGroupActivityResponse from(AdminStudyGroupActivityInfo info) {
        return AdminStudyGroupActivityResponse.builder()
            .totalGroupCount(info.totalGroupCount())
            .totalScheduleCount(info.totalScheduleCount())
            .byPart(info.byPart().stream().map(PartActivityResponse::from).toList())
            .build();
    }

    @Builder
    public record PartActivityResponse(
        ChallengerPart part,
        long groupCount,
        long totalScheduleCount,
        double averageScheduleCount,
        List<NoScheduleGroupResponse> noScheduleGroups
    ) {

        public static PartActivityResponse from(AdminStudyGroupActivityInfo.PartActivityInfo info) {
            return PartActivityResponse.builder()
                .part(info.part())
                .groupCount(info.groupCount())
                .totalScheduleCount(info.totalScheduleCount())
                .averageScheduleCount(info.averageScheduleCount())
                .noScheduleGroups(info.noScheduleGroups().stream().map(NoScheduleGroupResponse::from).toList())
                .build();
        }
    }

    @Builder
    public record NoScheduleGroupResponse(
        Long studyGroupId,
        String name
    ) {

        public static NoScheduleGroupResponse from(AdminStudyGroupActivityInfo.NoScheduleGroupInfo info) {
            return NoScheduleGroupResponse.builder()
                .studyGroupId(info.studyGroupId())
                .name(info.name())
                .build();
        }
    }
}
