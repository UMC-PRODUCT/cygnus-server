package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

public record RecruitingPublicRoundGroupInfo(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    List<RecruitingPublicRoundInfo> rounds
) {

    public RecruitingPublicRoundGroupInfo {
        rounds = List.copyOf(rounds);
    }
}
