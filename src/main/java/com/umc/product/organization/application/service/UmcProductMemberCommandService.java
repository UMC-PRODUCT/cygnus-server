package com.umc.product.organization.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductPartMembershipCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductPart;
import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductMemberCommandService implements ManageUmcProductMemberUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final SaveUmcProductMemberPort saveUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final SaveUmcProductMemberActivityPeriodPort saveUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductPartPort loadUmcProductPartPort;
    private final LoadUmcProductPartMembershipPort loadUmcProductPartMembershipPort;
    private final SaveUmcProductPartMembershipPort saveUmcProductPartMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final SaveUmcProductLeadershipPort saveUmcProductLeadershipPort;
    private final LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    private final SaveUmcProductSquadParticipantPort saveUmcProductSquadParticipantPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetFileUseCase getFileUseCase;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductMember",
        targetId = "#result",
        description = "'UMC Product 멤버를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductMemberCommand command) {
        validateCanManage(command.requesterMemberId());
        getMemberUseCase.getById(command.memberId());
        validateMemberNotDuplicated(command.memberId());
        validateProfileImage(command.profileImageId());
        validateInitialActivityPeriods(command.activityPeriods());

        UmcProductMember member = saveUmcProductMemberPort.save(UmcProductMember.create(
            command.memberId(), command.introduction(), command.profileImageId()
        ));
        command.activityPeriods().stream()
            .map(period -> UmcProductMemberActivityPeriod.create(member, period.startDate(), period.endDate()))
            .forEach(saveUmcProductMemberActivityPeriodPort::save);
        return member.getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMember",
        targetId = "#command.umcProductMemberId()",
        description = "'UMC Product 멤버 프로필을 수정했습니다.'"
    )
    @Override
    public void updateProfile(UpdateUmcProductMemberProfileCommand command) {
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        if (!umcProductAccessPolicy.canManageMemberProfile(command.requesterMemberId(), member.getMemberId())) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
        validateProfileImage(command.profileImageId());
        member.updateProfile(command.introduction(), command.profileImageId());
        saveUmcProductMemberPort.save(member);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductMember",
        targetId = "#umcProductMemberId",
        description = "'UMC Product 멤버를 삭제했습니다.'"
    )
    public void delete(Long umcProductMemberId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        saveUmcProductSquadParticipantPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductPartMembershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductLeadershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberActivityPeriodPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberPort.delete(member);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductMemberActivityPeriod",
        targetId = "#result",
        description = "'UMC Product 멤버 활동 기간을 생성했습니다.'"
    )
    public Long createActivityPeriod(CreateUmcProductMemberActivityPeriodCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        validatePeriod(command.startDate(), command.endDate());
        validateActivityPeriodNotOverlapped(member.getId(), command.startDate(), command.endDate(), null);
        return saveUmcProductMemberActivityPeriodPort.save(UmcProductMemberActivityPeriod.create(
            member, command.startDate(), command.endDate()
        )).getId();
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMemberActivityPeriod",
        targetId = "#command.activityPeriodId()",
        description = "'UMC Product 멤버 활동 기간을 수정했습니다.'"
    )
    public void updateActivityPeriod(UpdateUmcProductMemberActivityPeriodCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductMemberActivityPeriod activityPeriod = loadUmcProductMemberActivityPeriodPort
            .getById(command.activityPeriodId());
        validateOwnedBy(activityPeriod, member.getId());
        validatePeriod(command.startDate(), command.endDate());
        validateActivityPeriodNotOverlapped(
            member.getId(), command.startDate(), command.endDate(), activityPeriod.getId()
        );
        validateChildActivitiesContained(activityPeriod, command.startDate(), command.endDate());
        activityPeriod.updatePeriod(command.startDate(), command.endDate());
        saveUmcProductMemberActivityPeriodPort.save(activityPeriod);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductMemberActivityPeriod",
        targetId = "#activityPeriodId",
        description = "'UMC Product 멤버 활동 기간을 삭제했습니다.'"
    )
    public void deleteActivityPeriod(Long umcProductMemberId, Long activityPeriodId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        UmcProductMemberActivityPeriod activityPeriod = loadUmcProductMemberActivityPeriodPort
            .getById(activityPeriodId);
        validateOwnedBy(activityPeriod, member.getId());
        if (loadUmcProductPartMembershipPort.existsByMemberActivityPeriodId(activityPeriodId)
            || loadUmcProductLeadershipPort.existsByMemberActivityPeriodId(activityPeriodId)
            || loadUmcProductSquadParticipantPort.existsByMemberActivityPeriodId(activityPeriodId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS);
        }
        saveUmcProductMemberActivityPeriodPort.delete(activityPeriod);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductPartMembership",
        targetId = "#result",
        description = "'UMC Product Part 소속을 생성했습니다.'"
    )
    public Long createPartMembership(CreateUmcProductPartMembershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductPart part = loadUmcProductPartPort.getById(command.partId());
        validatePartMembershipNotOverlapped(command, member.getId(), null);
        UmcProductPartMembership membership = UmcProductPartMembership.create(
            activityPeriod,
            part,
            command.role(),
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        return saveUmcProductPartMembershipPort.save(membership).getId();
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductPartMembership",
        targetId = "#command.partMembershipId()",
        description = "'UMC Product Part 소속을 수정했습니다.'"
    )
    public void updatePartMembership(UpdateUmcProductPartMembershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductPartMembership membership = loadUmcProductPartMembershipPort.getById(command.partMembershipId());
        validateOwnedBy(membership, member.getId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductPart part = loadUmcProductPartPort.getById(command.partId());
        validatePartMembershipNotOverlapped(command, member.getId(), membership.getId());
        membership.update(
            activityPeriod,
            part,
            command.role(),
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        saveUmcProductPartMembershipPort.save(membership);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductPartMembership",
        targetId = "#partMembershipId",
        description = "'UMC Product Part 소속을 삭제했습니다.'"
    )
    public void deletePartMembership(Long umcProductMemberId, Long partMembershipId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        UmcProductPartMembership membership = loadUmcProductPartMembershipPort.getById(partMembershipId);
        validateOwnedBy(membership, member.getId());
        saveUmcProductPartMembershipPort.delete(membership);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductLeadership",
        targetId = "#result",
        description = "'UMC Product Leadership을 생성했습니다.'"
    )
    public Long createLeadership(CreateUmcProductLeadershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        validateLeadershipNotOverlapped(
            member.getId(), command.role(), command.startDate(), command.endDate(), null
        );
        return saveUmcProductLeadershipPort.save(UmcProductLeadership.create(
            activityPeriod, command.role(), command.startDate(), command.endDate()
        )).getId();
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductLeadership",
        targetId = "#command.leadershipId()",
        description = "'UMC Product Leadership을 수정했습니다.'"
    )
    public void updateLeadership(UpdateUmcProductLeadershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductLeadership leadership = loadUmcProductLeadershipPort.getById(command.leadershipId());
        validateOwnedBy(leadership, member.getId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        validateLeadershipNotOverlapped(
            member.getId(), command.role(), command.startDate(), command.endDate(), leadership.getId()
        );
        leadership.update(activityPeriod, command.role(), command.startDate(), command.endDate());
        saveUmcProductLeadershipPort.save(leadership);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductLeadership",
        targetId = "#leadershipId",
        description = "'UMC Product Leadership을 삭제했습니다.'"
    )
    public void deleteLeadership(Long umcProductMemberId, Long leadershipId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        UmcProductLeadership leadership = loadUmcProductLeadershipPort.getById(leadershipId);
        validateOwnedBy(leadership, member.getId());
        saveUmcProductLeadershipPort.delete(leadership);
    }

    private void validateInitialActivityPeriods(List<UmcProductActivityPeriodCommand> periods) {
        if (periods == null || periods.isEmpty()) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        }
        periods.forEach(period -> validatePeriod(period.startDate(), period.endDate()));
        List<UmcProductActivityPeriodCommand> sorted = periods.stream()
            .sorted(Comparator.comparing(UmcProductActivityPeriodCommand::startDate))
            .toList();
        for (int index = 1; index < sorted.size(); index++) {
            LocalDate previousEnd = sorted.get(index - 1).endDate();
            LocalDate nextStart = sorted.get(index).startDate();
            if (previousEnd == null || !nextStart.isAfter(nextDay(previousEnd))) {
                throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
            }
        }
    }

    private void validateActivityPeriodNotOverlapped(
        Long memberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedActivityPeriodId
    ) {
        if (loadUmcProductMemberActivityPeriodPort.existsOverlappingOrAdjacent(
            memberId, startDate, endDate, excludedActivityPeriodId
        )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
        }
    }

    private void validateChildActivitiesContained(
        UmcProductMemberActivityPeriod activityPeriod,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Long memberId = activityPeriod.getUmcProductMember().getId();
        boolean outOfRange = loadUmcProductPartMembershipPort.listByUmcProductMemberId(memberId).stream()
            .filter(item -> Objects.equals(item.getMemberActivityPeriod().getId(), activityPeriod.getId()))
            .anyMatch(item -> !contains(startDate, endDate, item.getStartDate(), item.getEndDate()))
            || loadUmcProductLeadershipPort.listByUmcProductMemberId(memberId).stream()
            .filter(item -> Objects.equals(item.getMemberActivityPeriod().getId(), activityPeriod.getId()))
            .anyMatch(item -> !contains(startDate, endDate, item.getStartDate(), item.getEndDate()))
            || loadUmcProductSquadParticipantPort.listByUmcProductMemberId(memberId).stream()
            .filter(item -> Objects.equals(item.getMemberActivityPeriod().getId(), activityPeriod.getId()))
            .anyMatch(item -> !contains(startDate, endDate, item.getStartDate(), item.getEndDate()));
        if (outOfRange) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }

    private void validatePartMembershipNotOverlapped(
        CreateUmcProductPartMembershipCommand command,
        Long memberId,
        Long excludedPartMembershipId
    ) {
        validatePartMembershipNotOverlapped(
            memberId, command.partId(), command.role(), command.position(), command.responsibilityTitle(),
            command.startDate(), command.endDate(), excludedPartMembershipId
        );
    }

    private void validatePartMembershipNotOverlapped(
        UpdateUmcProductPartMembershipCommand command,
        Long memberId,
        Long excludedPartMembershipId
    ) {
        validatePartMembershipNotOverlapped(
            memberId, command.partId(), command.role(), command.position(), command.responsibilityTitle(),
            command.startDate(), command.endDate(), excludedPartMembershipId
        );
    }

    private void validatePartMembershipNotOverlapped(
        Long memberId,
        Long partId,
        UmcProductPartRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedPartMembershipId
    ) {
        if (loadUmcProductPartMembershipPort.existsOverlappingSameAssignment(
            memberId,
            partId,
            role,
            position,
            normalizeNullable(responsibilityTitle),
            startDate,
            endDate,
            excludedPartMembershipId
        )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_MEMBERSHIP_OVERLAPPED);
        }
        if (role == UmcProductPartRole.PART_LEAD
            && loadUmcProductPartMembershipPort.existsOverlappingPartLead(
                partId, startDate, endDate, excludedPartMembershipId
            )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_LEAD_OVERLAPPED);
        }
    }

    private void validateLeadershipNotOverlapped(
        Long memberId,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedLeadershipId
    ) {
        if (loadUmcProductLeadershipPort.existsOverlappingRole(
            role, startDate, endDate, excludedLeadershipId
        ) || loadUmcProductLeadershipPort.existsOverlappingMember(
            memberId, startDate, endDate, excludedLeadershipId
        )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED);
        }
    }

    private UmcProductMemberActivityPeriod getContainingPeriod(
        Long memberId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return loadUmcProductMemberActivityPeriodPort.findContaining(memberId, startDate, endDate)
            .orElseThrow(() -> new OrganizationDomainException(
                OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE
            ));
    }

    private void validateOwnedBy(UmcProductMemberActivityPeriod activityPeriod, Long memberId) {
        if (!Objects.equals(activityPeriod.getUmcProductMember().getId(), memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND);
        }
    }

    private void validateOwnedBy(UmcProductPartMembership membership, Long memberId) {
        if (!Objects.equals(membership.getMemberActivityPeriod().getUmcProductMember().getId(), memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_MEMBERSHIP_NOT_FOUND);
        }
    }

    private void validateOwnedBy(UmcProductLeadership leadership, Long memberId) {
        if (!Objects.equals(leadership.getMemberActivityPeriod().getUmcProductMember().getId(), memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_NOT_FOUND);
        }
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }

    private void validateMemberNotDuplicated(Long memberId) {
        if (loadUmcProductMemberPort.existsByMemberId(memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_ALREADY_EXISTS);
        }
    }

    private void validateProfileImage(String profileImageId) {
        if (profileImageId != null && !profileImageId.isBlank() && !getFileUseCase.existsById(profileImageId)) {
            throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
        }
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
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

    private static LocalDate nextDay(LocalDate date) {
        return date.equals(LocalDate.MAX) ? LocalDate.MAX : date.plusDays(1);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
