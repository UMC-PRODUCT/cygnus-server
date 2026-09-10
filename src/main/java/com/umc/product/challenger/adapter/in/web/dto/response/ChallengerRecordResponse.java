package com.umc.product.challenger.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

@Builder
public record ChallengerRecordResponse(
    String code,
    ChallengerPart part,
    ChallengerTrack track,
    Long gisuId,
    Long gisu,
    Long schoolId,
    String schoolName,
    Long chapterId,
    String chapterName,
    String memberName,
    ChallengerRoleType challengerRoleType,
    Long organizationId,
    List<ChallengerTrack> tracks
) {
    public ChallengerRecordResponse {
        tracks = tracks == null ? (track == null ? List.of() : List.of(track)) : List.copyOf(tracks);
        track = tracks.size() == 1 ? tracks.getFirst() : null;
    }

    public ChallengerRecordResponse(
        String code, ChallengerPart part, ChallengerTrack track, Long gisuId, Long gisu, Long schoolId,
        String schoolName, Long chapterId, String chapterName, String memberName,
        ChallengerRoleType challengerRoleType, Long organizationId
    ) {
        this(code, part, track, gisuId, gisu, schoolId, schoolName, chapterId, chapterName, memberName,
            challengerRoleType, organizationId, null);
    }

    public ChallengerRecordResponse(
        String code, ChallengerPart part, Long gisuId, Long gisu, Long schoolId, String schoolName,
        Long chapterId, String chapterName, String memberName, ChallengerRoleType challengerRoleType,
        Long organizationId
    ) {
        this(code, part, null, gisuId, gisu, schoolId, schoolName, chapterId, chapterName, memberName,
            challengerRoleType, organizationId);
    }
}
