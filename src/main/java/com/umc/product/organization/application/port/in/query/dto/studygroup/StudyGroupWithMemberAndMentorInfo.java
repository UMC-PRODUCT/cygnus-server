package com.umc.product.organization.application.port.in.query.dto.studygroup;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record StudyGroupWithMemberAndMentorInfo(
    Long groupId,
    String name,
    Long gisuId,
    ChallengerPart part,
    Instant createdAt,
    List<StudyGroupMemberInfo> mentors,
    List<StudyGroupMemberInfo> members,
    ChallengerTrack track
) {
    public StudyGroupWithMemberAndMentorInfo(
        Long groupId, String name, Long gisuId, ChallengerPart part,
        Instant createdAt,
        List<StudyGroupMemberInfo> mentors,
        List<StudyGroupMemberInfo> members
    ) {
        this(groupId, name, gisuId, part, createdAt, mentors, members, null);
    }

    public StudyGroupWithMemberAndMentorInfo {
        mentors = mentors == null ? List.of() : List.copyOf(mentors);
        members = members == null ? List.of() : List.copyOf(members);
    }

    public static StudyGroupWithMemberAndMentorInfo create(
        Long groupId, String name,
        Long gisuId, ChallengerPart part,
        Instant createdAt,
        List<StudyGroupMemberInfo> mentors, List<StudyGroupMemberInfo> members
    ) {
        return create(groupId, name, gisuId, part, createdAt, mentors, members, null);
    }

    public static StudyGroupWithMemberAndMentorInfo create(
        Long groupId, String name, Long gisuId, ChallengerPart part, Instant createdAt,
        List<StudyGroupMemberInfo> mentors, List<StudyGroupMemberInfo> members, ChallengerTrack track
    ) {
        return new StudyGroupWithMemberAndMentorInfo(
            groupId, name,
            gisuId, part,
            createdAt,
            mentors, members, track
        );
    }
}
