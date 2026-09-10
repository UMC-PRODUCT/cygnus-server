package com.umc.product.organization.application.port.in.command.dto;

import java.util.Objects;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record CreateStudyGroupCommand(
        String name,
        Long gisuId,
        ChallengerPart part,
        Set<Long> mentorIds,
        Set<Long> memberIds,
        ChallengerTrack track
) {
    public CreateStudyGroupCommand(
        String name, Long gisuId, ChallengerPart part, Set<Long> mentorIds, Set<Long> memberIds
    ) {
        this(name, gisuId, part, mentorIds, memberIds, null);
    }

    public CreateStudyGroupCommand {
        Objects.requireNonNull(name, "스터디 그룹 이름은 필수입니다.");
        Objects.requireNonNull(gisuId, "기수 ID는 필수입니다.");
        mentorIds = mentorIds != null ? Set.copyOf(mentorIds) : mentorIds;
        memberIds = memberIds != null ? Set.copyOf(memberIds) : memberIds;
    }
}
