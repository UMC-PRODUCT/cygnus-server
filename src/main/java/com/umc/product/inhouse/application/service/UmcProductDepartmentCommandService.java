package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductDepartmentUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductDepartmentParticipantCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentParticipantCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductDepartmentCommandService implements ManageUmcProductDepartmentUseCase {

    private final LoadUmcProductDepartmentPort loadUmcProductDepartmentPort;
    private final SaveUmcProductDepartmentPort saveUmcProductDepartmentPort;
    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    private final SaveUmcProductDepartmentParticipantPort saveUmcProductDepartmentParticipantPort;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.CREATE,
        targetType = "UmcProductDepartment",
        targetId = "#result",
        description = "'UMC PRODUCT Department를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductDepartmentCommand command) {
        validateCanManage(command.requesterMemberId());
        Map<Long, UmcProductDepartment> departmentTree = lockDepartmentTree();
        validateCodeNotDuplicated(command.code(), null);
        UmcProductDepartment parent = resolveParent(command.parentDepartmentId(), null, departmentTree);
        UmcProductDepartment department = UmcProductDepartment.create(
            command.code(),
            command.name(),
            command.description(),
            parent,
            command.startDate(),
            command.endDate(),
            command.sortOrder(),
            command.active()
        );
        return saveUmcProductDepartmentPort.save(department).getId();
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.UPDATE,
        targetType = "UmcProductDepartment",
        targetId = "#command.departmentId()",
        description = "'UMC PRODUCT Department를 수정했습니다.'"
    )
    @Override
    public void update(UpdateUmcProductDepartmentCommand command) {
        validateCanManage(command.requesterMemberId());
        Map<Long, UmcProductDepartment> departmentTree = lockDepartmentTree();
        UmcProductDepartment department = getFromTree(command.departmentId(), departmentTree);
        UmcProductDepartment parent = resolveParent(
            command.parentDepartmentId(),
            department.getId(),
            departmentTree
        );
        if (command.code() != null) {
            validateCodeNotDuplicated(command.code(), department.getId());
        }
        LocalDate nextStartDate = command.startDate() != null ? command.startDate() : department.getStartDate();
        validatePeriod(nextStartDate, command.endDate());
        validateParticipantsContained(department.getId(), nextStartDate, command.endDate());
        department.update(
            command.code(),
            command.name(),
            command.description(),
            parent,
            nextStartDate,
            command.endDate(),
            command.sortOrder(),
            command.active()
        );
        saveUmcProductDepartmentPort.save(department);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.DELETE,
        targetType = "UmcProductDepartment",
        targetId = "#departmentId",
        description = "'UMC PRODUCT Department를 삭제했습니다.'"
    )
    @Override
    public void delete(Long departmentId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        Map<Long, UmcProductDepartment> departmentTree = lockDepartmentTree();
        UmcProductDepartment department = getFromTree(departmentId, departmentTree);
        if (loadUmcProductDepartmentPort.existsByParentId(departmentId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_HAS_CHILDREN);
        }
        if (loadUmcProductDepartmentParticipantPort.existsByDepartmentId(departmentId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_HAS_PARTICIPANTS);
        }
        saveUmcProductDepartmentPort.delete(department);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.CREATE,
        targetType = "UmcProductDepartmentParticipant",
        targetId = "#result",
        description = "'UMC PRODUCT Department 참여 이력을 생성했습니다.'"
    )
    @Override
    public Long createParticipant(CreateUmcProductDepartmentParticipantCommand command) {
        validateCanManage(command.requesterMemberId());
        validatePeriod(command.startDate(), command.endDate());

        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductDepartment department = loadUmcProductDepartmentPort.getByIdWithLock(command.departmentId());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductDepartmentParticipant participant = UmcProductDepartmentParticipant.create(
            department,
            activityPeriod,
            command.role(),
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        validateParticipationNotOverlapped(
            department.getId(),
            member.getId(),
            participant.getRole(),
            participant.getStartDate(),
            participant.getEndDate(),
            null
        );
        return saveUmcProductDepartmentParticipantPort.save(participant).getId();
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.UPDATE,
        targetType = "UmcProductDepartmentParticipant",
        targetId = "#command.participantId()",
        description = "'UMC PRODUCT Department 참여 이력을 수정했습니다.'"
    )
    @Override
    public void updateParticipant(UpdateUmcProductDepartmentParticipantCommand command) {
        validateCanManage(command.requesterMemberId());
        validatePeriod(command.startDate(), command.endDate());

        UmcProductDepartmentParticipant participant = loadUmcProductDepartmentParticipantPort.getById(command.participantId());
        Long memberId = participant.getMemberActivityPeriod().getUmcProductMember().getId();
        loadUmcProductMemberPort.getByIdWithLock(memberId);
        UmcProductDepartment department = loadUmcProductDepartmentPort.getByIdWithLock(command.departmentId());
        validateBelongsToDepartment(participant, department.getId());

        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            memberId, command.startDate(), command.endDate()
        );
        validateParticipationNotOverlapped(
            department.getId(),
            memberId,
            command.role(),
            command.startDate(),
            command.endDate(),
            participant.getId()
        );
        participant.update(
            activityPeriod,
            command.role(),
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        saveUmcProductDepartmentParticipantPort.save(participant);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.DELETE,
        targetType = "UmcProductDepartmentParticipant",
        targetId = "#participantId",
        description = "'UMC PRODUCT Department 참여 이력을 삭제했습니다.'"
    )
    @Override
    public void deleteParticipant(Long departmentId, Long participantId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductDepartmentParticipant participant = loadUmcProductDepartmentParticipantPort.getById(participantId);
        Long memberId = participant.getMemberActivityPeriod().getUmcProductMember().getId();
        loadUmcProductMemberPort.getByIdWithLock(memberId);
        UmcProductDepartment department = loadUmcProductDepartmentPort.getByIdWithLock(departmentId);
        validateBelongsToDepartment(participant, department.getId());
        saveUmcProductDepartmentParticipantPort.delete(participant);
    }

    private void validateParticipantsContained(Long departmentId, LocalDate startDate, LocalDate endDate) {
        boolean outOfRange = loadUmcProductDepartmentParticipantPort.listByDepartmentId(departmentId).stream()
            .anyMatch(participant -> !contains(
                startDate,
                endDate,
                participant.getStartDate(),
                participant.getEndDate()
            ));
        if (outOfRange) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }

    private void validateParticipationNotOverlapped(
        Long departmentId,
        Long memberId,
        UmcProductDepartmentRole role,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedParticipantId
    ) {
        if (loadUmcProductDepartmentParticipantPort.existsOverlappingMemberInDepartment(
            departmentId, memberId, startDate, endDate, excludedParticipantId
        )) {
            throw new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPATION_OVERLAPPED
            );
        }
        if (role == UmcProductDepartmentRole.DEPARTMENT_LEAD
            && loadUmcProductDepartmentParticipantPort.existsOverlappingDepartmentLead(
                departmentId, startDate, endDate, excludedParticipantId
            )) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_LEAD_OVERLAPPED);
        }
    }

    private UmcProductMemberActivityPeriod getContainingPeriod(
        Long memberId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return loadUmcProductMemberActivityPeriodPort.findContaining(memberId, startDate, endDate)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE
            ));
    }

    private void validateBelongsToDepartment(UmcProductDepartmentParticipant participant, Long departmentId) {
        if (!Objects.equals(participant.getDepartment().getId(), departmentId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }

    private void validateCodeNotDuplicated(String code, Long excludedDepartmentId) {
        if (code != null && loadUmcProductDepartmentPort.existsByCode(code.trim(), excludedDepartmentId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_ALREADY_EXISTS);
        }
    }

    private Map<Long, UmcProductDepartment> lockDepartmentTree() {
        return loadUmcProductDepartmentPort.listAllWithLock().stream()
            .collect(Collectors.toMap(UmcProductDepartment::getId, Function.identity()));
    }

    private UmcProductDepartment getFromTree(
        Long departmentId,
        Map<Long, UmcProductDepartment> departmentTree
    ) {
        UmcProductDepartment department = departmentTree.get(departmentId);
        if (department == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_NOT_FOUND);
        }
        return department;
    }

    private UmcProductDepartment resolveParent(
        Long parentDepartmentId,
        Long selfId,
        Map<Long, UmcProductDepartment> departmentTree
    ) {
        if (parentDepartmentId == null) {
            return null;
        }
        Set<Long> visited = new HashSet<>();
        Long cursorId = parentDepartmentId;
        while (cursorId != null) {
            if (Objects.equals(cursorId, selfId) || !visited.add(cursorId)) {
                throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_CYCLE);
            }
            UmcProductDepartment cursor = getFromTree(cursorId, departmentTree);
            cursorId = cursor.getParent() == null ? null : cursor.getParent().getId();
        }
        return getFromTree(parentDepartmentId, departmentTree);
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_PERIOD_INVALID);
        }
    }

    private static boolean contains(
        LocalDate parentStart,
        LocalDate parentEnd,
        LocalDate childStart,
        LocalDate childEnd
    ) {
        return !childStart.isBefore(parentStart)
            && (parentEnd == null || childEnd != null && !childEnd.isAfter(parentEnd));
    }
}
