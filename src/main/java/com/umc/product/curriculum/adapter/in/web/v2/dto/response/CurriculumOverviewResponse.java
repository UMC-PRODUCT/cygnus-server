package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;

import lombok.Builder;

@Builder
public record CurriculumOverviewResponse(
    Long curriculumId,
    String title,
    List<WeeklyCurriculumOverviewResponse> weeks,
    ChallengerPart part,
    ChallengerTrack track
) {

    public CurriculumOverviewResponse(Long curriculumId, String title, List<WeeklyCurriculumOverviewResponse> weeks) {
        this(curriculumId, title, weeks, null, null);
    }

    public static CurriculumOverviewResponse from(CurriculumOverviewInfo info) {
        return CurriculumOverviewResponse.builder()
            .curriculumId(info.curriculumId())
            .title(info.title())
            .part(info.part())
            .track(info.track())
            .weeks(info.weeks().stream()
                .map(w -> WeeklyCurriculumOverviewResponse.builder()
                    .weeklyCurriculumId(w.weeklyCurriculumId())
                    .weekNo(w.weekNo())
                    .title(w.title())
                    .isExtra(w.isExtra())
                    .startsAt(w.startsAt())
                    .endsAt(w.endsAt())
                    .build())
                .toList())
            .build();
    }

    @Builder
    public record WeeklyCurriculumOverviewResponse(
        Long weeklyCurriculumId,
        Long weekNo,
        String title,
        boolean isExtra,
        Instant startsAt,
        Instant endsAt
    ) {

    }
}
