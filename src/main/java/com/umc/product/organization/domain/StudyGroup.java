package com.umc.product.organization.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "study_group")
public class StudyGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private Long gisuId;

    @Enumerated(EnumType.STRING)
    private ChallengerPart part;

    @Enumerated(EnumType.STRING)
    private ChallengerTrack track;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyGroupMember> members = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyGroupMentor> mentors = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private StudyGroup(String name, Long gisuId, ChallengerPart part, ChallengerTrack track) {
        validate(name, gisuId, part, track);
        this.name = name;
        this.gisuId = gisuId;
        this.part = part;
        this.track = track;
    }

    public static StudyGroup create(
        String name, Long gisuId, ChallengerPart part
    ) {
        return StudyGroup.builder()
            .name(name)
            .gisuId(gisuId)
            .part(part)
            .build();
    }

    public static StudyGroup create(
        String name, Long gisuId, ChallengerPart part,
        Set<Long> memberIds, Set<Long> mentorIds
    ) {
        StudyGroup studyGroup = create(name, gisuId, part);
        studyGroup.addMembers(memberIds);
        studyGroup.assignMentors(mentorIds);
        return studyGroup;
    }

    public static StudyGroup create(
        String name, Long gisuId, ChallengerPart part, ChallengerTrack track,
        Set<Long> memberIds, Set<Long> mentorIds
    ) {
        StudyGroup studyGroup = StudyGroup.builder()
            .name(name).gisuId(gisuId).part(part).track(track).build();
        studyGroup.addMembers(memberIds);
        studyGroup.assignMentors(mentorIds);
        return studyGroup;
    }

    private static void validate(String name, Long gisuId, ChallengerPart part, ChallengerTrack track) {
        if (name == null || name.isBlank()) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_NAME_REQUIRED);
        }

        if (gisuId == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.GISU_REQUIRED);
        }

        if (part == null && track == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.PART_REQUIRED);
        }
        if ((part != null && track != null) || (track != null && !track.isBasic())) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
        }
    }

    // ============ Domain Methods (Aggregate Root Pattern) ============

    public void updateName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public void updatePart(ChallengerPart challengerPart) {
        if (challengerPart != null && track != null) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_LEARNING_TYPE_INVALID);
        }
        if (challengerPart != null) {
            this.part = challengerPart;
        }
    }

    public void addMembers(Set<Long> memberIds) {
        validateMembersRequired(memberIds);
        memberIds.forEach(this::addMember);
    }

    public void assignMentors(Set<Long> mentorIds) {
        validateMentorsRequired(mentorIds);
        mentorIds.forEach(this::assignMentor);
    }

    public void addMember(Long memberId) {
        if (hasMember(memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MEMBER_DUPLICATED);
        }
        members.add(StudyGroupMember.create(this, memberId));
    }

    public void assignMentor(Long mentorId) {
        if (hasMentor(mentorId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MENTOR_DUPLICATED);
        }
        mentors.add(StudyGroupMentor.create(this, mentorId));
    }

    public void removeMember(Long memberId) {
        StudyGroupMember targetMember = members.stream()
            .filter(member -> member.isSameMember(memberId))
            .findFirst()
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MEMBER_NOT_FOUND));

        if (members.size() == 1) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED);
        }

        members.remove(targetMember);
    }

    public void removeMentor(Long mentorId) {
        StudyGroupMentor targetMentor = mentors.stream()
            .filter(mentor -> mentor.isSameMember(mentorId))
            .findFirst()
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MENTOR_NOT_FOUND));

        if (mentors.size() == 1) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MENTOR_REQUIRED);
        }

        mentors.remove(targetMentor);
    }

    public boolean hasMember(Long memberId) {
        return members.stream()
            .anyMatch(member -> member.isSameMember(memberId));
    }

    public List<StudyGroupMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<StudyGroupMentor> getMentors() {
        return Collections.unmodifiableList(mentors);
    }

    private boolean hasMentor(Long mentorId) {
        return mentors.stream()
            .anyMatch(mentor -> mentor.isSameMember(mentorId));
    }

    private static void validateMembersRequired(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MEMBER_REQUIRED);
        }
    }

    private static void validateMentorsRequired(Set<Long> mentorIds) {
        if (mentorIds == null || mentorIds.isEmpty()) {
            throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_MENTOR_REQUIRED);
        }
    }
}
