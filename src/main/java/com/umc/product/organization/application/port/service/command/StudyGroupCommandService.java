package com.umc.product.organization.application.port.service.command;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.application.port.in.command.ManageStudyGroupUseCase;
import com.umc.product.organization.application.port.in.command.dto.AddStudyMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.AddStudyMentorCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupCommand;
import com.umc.product.organization.application.port.in.command.dto.DeleteStudyMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.DeleteStudyMentorCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateStudyGroupCommand;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.StudyGroupMember;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyGroupCommandService implements ManageStudyGroupUseCase {

    private final LoadStudyGroupPort loadStudyGroupPort;
    private final LoadGisuPort loadGisuPort;
    private final SaveStudyGroupPort saveStudyGroupPort;

    @Override
    public void create(CreateStudyGroupCommand command) {
        // 생성하고자 하는 기수에 스터디를 생성
        Gisu gisu = loadGisuPort.getById(command.gisuId());
        validateNoPartStudyConflict(gisu.getId(), command.part(), command.memberIds(), null);

        saveStudyGroupPort.save(
            StudyGroup.create(
                command.name(), gisu.getId(), command.part(),
                command.memberIds(), command.mentorIds()
            )
        );
    }

    @Override
    public void update(UpdateStudyGroupCommand command) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(command.groupId());

        if (command.part() != null && command.part() != studyGroup.getPart()) {
            Set<Long> memberIds = studyGroup.getMembers().stream()
                .map(StudyGroupMember::getMemberId)
                .collect(Collectors.toSet());
            validateNoPartStudyConflict(studyGroup.getGisuId(), command.part(), memberIds, studyGroup.getId());
        }

        studyGroup.updateName(command.name());
        studyGroup.updatePart(command.part());

        saveStudyGroupPort.save(studyGroup);
    }

    @Override
    public void addMember(AddStudyMemberCommand command) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(command.groupId());

        validateNoPartStudyConflict(
            studyGroup.getGisuId(), studyGroup.getPart(), Set.of(command.memberId()), studyGroup.getId()
        );
        studyGroup.addMember(command.memberId());
        saveStudyGroupPort.save(studyGroup);
    }

    @Override
    public void addMentor(AddStudyMentorCommand command) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(command.groupId());

        studyGroup.assignMentor(command.mentorId());
        saveStudyGroupPort.save(studyGroup);
    }

    @Override
    public void deleteMember(DeleteStudyMemberCommand command) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(command.groupId());

        studyGroup.removeMember(command.memberId());
        saveStudyGroupPort.save(studyGroup);
    }

    @Override
    public void deleteMentor(DeleteStudyMentorCommand command) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(command.groupId());

        studyGroup.removeMentor(command.mentorId());
        saveStudyGroupPort.save(studyGroup);
    }

    @Override
    public void delete(Long studyGroupId) {
        StudyGroup studyGroup = loadStudyGroupPort.getEntityById(studyGroupId);

        saveStudyGroupPort.delete(studyGroup);
    }

    private void validateNoPartStudyConflict(
        Long gisuId, ChallengerPart part, Set<Long> memberIds, Long excludedStudyGroupId
    ) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        // memberId의 목록에 들어있는 회원이 해당 기수에, 동일한 파트의 스터디에 참여하고 있는지를 검사
        Set<Long> conflictMemberIds =
            loadStudyGroupPort.findConflictedMemberIds(gisuId, part, memberIds, excludedStudyGroupId);
        if (!conflictMemberIds.isEmpty()) {
            throw new OrganizationDomainException(
                OrganizationErrorCode.STUDY_GROUP_MEMBER_ALREADY_IN_PART_STUDY,
                "제공된 회원 중에서 동일한 기수에 동일한 파트의 스터디에 참여하고 있는 회원이 있습니다. ID LIST: " + conflictMemberIds);
        }
    }
}
