package com.umc.product.curriculum.application.service.evaluator;

import java.util.HashSet;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurriculumStudyGroupStaffPolicy {

    private final GetStudyGroupUseCase getStudyGroupUseCase;
    private final GetMemberUseCase getMemberUseCase;

    public boolean canManage(
        SubjectAttributes subject,
        Long studyGroupId,
        Long targetMemberId,
        Long gisuId
    ) {
        AuthoritySnapshot snapshot = subject.toAuthoritySnapshot();
        if (snapshot.isSuperAdmin()) {
            return true;
        }

        if (studyGroupId != null) {
            StudyGroupInfo group = getStudyGroupUseCase.findById(studyGroupId).orElse(null);
            if (group == null || !Objects.equals(group.gisuId(), gisuId)) {
                return false;
            }
            if (group.mentorIds().contains(snapshot.memberId())
                && group.memberIds().contains(targetMemberId)) {
                return true;
            }
        }

        Long targetSchoolId = getMemberUseCase.findById(targetMemberId)
            .map(member -> member.schoolId())
            .orElse(null);
        return targetSchoolId != null && snapshot.isSchoolCoreInGisu(gisuId, targetSchoolId);
    }

    public boolean canManageGroup(SubjectAttributes subject, Long studyGroupId) {
        AuthoritySnapshot snapshot = subject.toAuthoritySnapshot();
        if (snapshot.isSuperAdmin()) {
            return true;
        }
        StudyGroupInfo group = getStudyGroupUseCase.findById(studyGroupId).orElse(null);
        if (group == null) {
            return false;
        }
        if (group.mentorIds().contains(snapshot.memberId())) {
            return true;
        }
        if (!snapshot.isSchoolCoreInGisu(group.gisuId(), snapshot.schoolId())) {
            return false;
        }
        return getMemberUseCase.findAllSchoolIdsByIds(new HashSet<>(group.memberIds())).values().stream()
            .anyMatch(snapshot.schoolId()::equals);
    }
}
