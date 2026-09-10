package com.umc.product.curriculum.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record CurriculumProjection(
    Long id,
    ChallengerPart part,
    ChallengerTrack track,
    String title
) {
    public CurriculumProjection(Long id, ChallengerPart part, String title) {
        this(id, part, null, title);
    }
}
