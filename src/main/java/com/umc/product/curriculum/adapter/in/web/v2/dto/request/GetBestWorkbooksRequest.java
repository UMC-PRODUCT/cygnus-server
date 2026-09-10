package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GetBestWorkbooksRequest(
    Long gisuId,
    Set<@Positive Long> schoolIds,
    Set<ChallengerPart> parts,
    List<@PositiveOrZero Long> weekNos,
    List<@Positive Long> studyGroupIds,
    @PositiveOrZero Integer page,
    @Positive @Max(100) Integer size,
    Set<ChallengerTrack> tracks
) {
    public GetBestWorkbooksRequest(
        Long gisuId, Set<Long> schoolIds, Set<ChallengerPart> parts, List<Long> weekNos,
        List<Long> studyGroupIds, Integer page, Integer size
    ) {
        this(gisuId, schoolIds, parts, weekNos, studyGroupIds, page, size, null);
    }

    public GetBestWorkbooksQuery toQuery() {
        return GetBestWorkbooksQuery.of(gisuId, schoolIds, parts, weekNos, studyGroupIds, page, size)
            .withTracks(tracks);
    }
}
