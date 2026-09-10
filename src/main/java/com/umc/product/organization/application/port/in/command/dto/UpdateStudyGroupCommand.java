package com.umc.product.organization.application.port.in.command.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record UpdateStudyGroupCommand(
        Long groupId,
        String name,
        ChallengerPart part,
    ChallengerTrack track
) {
    public UpdateStudyGroupCommand(Long groupId, String name, ChallengerPart part) {
        this(groupId, name, part, null);
    }

}
