package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminStudyGroupActivityInfo(
    long totalGroupCount,
    long totalScheduleCount,
    List<PartActivityInfo> byPart
) {

    public static AdminStudyGroupActivityInfo of(
        long totalGroupCount,
        long totalScheduleCount,
        List<PartActivityInfo> byPart
    ) {
        return AdminStudyGroupActivityInfo.builder()
            .totalGroupCount(totalGroupCount)
            .totalScheduleCount(totalScheduleCount)
            .byPart(List.copyOf(byPart))
            .build();
    }

    @Builder
    public record PartActivityInfo(
        ChallengerPart part,
        long groupCount,
        long totalScheduleCount,
        double averageScheduleCount,
        List<NoScheduleGroupInfo> noScheduleGroups
    ) {

        public static PartActivityInfo of(
            ChallengerPart part,
            long groupCount,
            long totalScheduleCount,
            double averageScheduleCount,
            List<NoScheduleGroupInfo> noScheduleGroups
        ) {
            return PartActivityInfo.builder()
                .part(part)
                .groupCount(groupCount)
                .totalScheduleCount(totalScheduleCount)
                .averageScheduleCount(averageScheduleCount)
                .noScheduleGroups(List.copyOf(noScheduleGroups))
                .build();
        }
    }

    @Builder
    public record NoScheduleGroupInfo(
        Long studyGroupId,
        String name
    ) {

        public static NoScheduleGroupInfo of(Long studyGroupId, String name) {
            return NoScheduleGroupInfo.builder()
                .studyGroupId(studyGroupId)
                .name(name)
                .build();
        }
    }
}
