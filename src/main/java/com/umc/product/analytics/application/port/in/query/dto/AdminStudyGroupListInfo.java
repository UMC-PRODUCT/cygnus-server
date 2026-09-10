package com.umc.product.analytics.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.Builder;

@Builder
public record AdminStudyGroupListInfo(
    List<PartGroupsInfo> byPart
) {

    public static AdminStudyGroupListInfo from(List<PartGroupsInfo> byPart) {
        return AdminStudyGroupListInfo.builder()
            .byPart(List.copyOf(byPart))
            .build();
    }

    @Builder
    public record PartGroupsInfo(
        ChallengerPart part,
        List<StudyGroupInfo> groups
    ) {

        public static PartGroupsInfo of(ChallengerPart part, List<StudyGroupInfo> groups) {
            return PartGroupsInfo.builder()
                .part(part)
                .groups(List.copyOf(groups))
                .build();
        }
    }

    @Builder
    public record StudyGroupInfo(
        Long studyGroupId,
        String name,
        long scheduleCount
    ) {

        public static StudyGroupInfo of(Long studyGroupId, String name, long scheduleCount) {
            return StudyGroupInfo.builder()
                .studyGroupId(studyGroupId)
                .name(name)
                .scheduleCount(scheduleCount)
                .build();
        }
    }
}
