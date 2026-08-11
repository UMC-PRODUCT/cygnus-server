package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

public record RecruitingPublicRoundSearchGraphQlRequest(
    String gisuId,
    String chapterId,
    List<String> schoolIds,
    List<String> roundIds,
    String schoolName,
    String seasonId,
    ChallengerTrack track,
    RecruitingRoundPhase phase,
    RecruitingRoundSort sort
) {

    public RecruitingPublicRoundSearchQuery toQuery() {
        return RecruitingPublicRoundSearchQuery.builder()
            .gisuId(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU))
            .chapterId(chapterId == null ? null : GlobalId.decodeLong(chapterId, GlobalIdTypes.CHAPTER))
            .schoolIds(schoolIds == null
                ? null
                : Set.copyOf(GlobalId.decodeLongs(schoolIds, GlobalIdTypes.SCHOOL)))
            .roundIds(roundIds == null
                ? null
                : Set.copyOf(GlobalId.decodeLongs(roundIds, GlobalIdTypes.RECRUITING_ROUND)))
            .schoolName(schoolName)
            .seasonId(seasonId == null ? null : GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON))
            .track(track)
            .phase(phase)
            .sort(sort)
            .build();
    }
}
