package com.umc.product.curriculum.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.domain.WeeklyCurriculum;

import lombok.Builder;

@Builder
public record CurriculumOverviewInfo(
    Long curriculumId,
    String title,
    List<WeeklyCurriculumOverviewInfo> weeks,
    ChallengerPart part,
    ChallengerTrack track
) {

    public CurriculumOverviewInfo(Long curriculumId, String title, List<WeeklyCurriculumOverviewInfo> weeks) {
        this(curriculumId, title, weeks, null, null);
    }

    public static CurriculumOverviewInfo of(CurriculumProjection projection, List<WeeklyCurriculumOverviewInfo> weeks) {
        return CurriculumOverviewInfo.builder()
            .curriculumId(projection.id())
            .title(projection.title())
            .part(projection.part())
            .track(projection.track())
            .weeks(weeks)
            .build();
    }

    @Builder
    public record WeeklyCurriculumOverviewInfo(
        Long weeklyCurriculumId,
        Long weekNo,
        String title,
        boolean isExtra,
        Instant startsAt,
        Instant endsAt
    ) {
        public static WeeklyCurriculumOverviewInfo of(WeeklyCurriculum wc) {
            return WeeklyCurriculumOverviewInfo.builder()
                .weeklyCurriculumId(wc.getId())
                .weekNo(wc.getWeekNo())
                .title(wc.getTitle())
                .isExtra(wc.isExtra())
                .startsAt(wc.getStartsAt())
                .endsAt(wc.getEndsAt())
                .build();
        }
    }
}
