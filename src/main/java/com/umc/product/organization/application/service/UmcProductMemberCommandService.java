package com.umc.product.organization.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.command.ManageUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.domain.FileUsageCoordinate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductMemberCommandService implements ManageUmcProductMemberUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final SaveUmcProductMemberPort saveUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final SaveUmcProductMemberActivityPeriodPort saveUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    private final SaveUmcProductChapterMembershipPort saveUmcProductChapterMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final SaveUmcProductLeadershipPort saveUmcProductLeadershipPort;
    private final LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    private final SaveUmcProductSquadParticipantPort saveUmcProductSquadParticipantPort;
    private final GetMemberUseCase getMemberUseCase;
    private final GetFileUseCase getFileUseCase;
    private final ManageFileUsageUseCase manageFileUsageUseCase;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductMember",
        targetId = "#result",
        description = "'UMC PRODUCT 멤버를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductMemberCommand command) {
        validateCanManage(command.requesterMemberId());
        getMemberUseCase.getById(command.memberId());
        validateMemberNotDuplicated(command.memberId());
        validateProfileImage(command.profileImageId(), command.requesterMemberId());
        validateInitialActivityPeriods(command.activityPeriods());

        UmcProductMember member = saveUmcProductMemberPort.save(UmcProductMember.create(
            command.memberId(), command.introduction(), command.profileImageId()
        ));
        command.activityPeriods().stream()
            .map(period -> UmcProductMemberActivityPeriod.create(member, period.startDate(), period.endDate()))
            .forEach(saveUmcProductMemberActivityPeriodPort::save);
        replaceProfileImageUsage(member, command.requesterMemberId());
        return member.getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMember",
        targetId = "#command.umcProductMemberId()",
        description = "'UMC PRODUCT 멤버 프로필을 수정했습니다.'"
    )
    @Override
    public void updateProfile(UpdateUmcProductMemberProfileCommand command) {
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        if (!umcProductAccessPolicy.canManageMemberProfile(command.requesterMemberId(), member.getMemberId())) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
        if (command.profileImageId() != null
            && !Objects.equals(member.getProfileImageId(), command.profileImageId())) {
            validateProfileImage(command.profileImageId(), command.requesterMemberId());
        }
        String profileImageId = command.profileImageId() == null
            ? member.getProfileImageId()
            : command.profileImageId();
        member.updateProfile(command.introduction(), profileImageId);
        UmcProductMember savedMember = saveUmcProductMemberPort.save(member);
        replaceProfileImageUsage(savedMember, command.requesterMemberId());
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductMember",
        targetId = "#umcProductMemberId",
        description = "'UMC PRODUCT 멤버를 삭제했습니다.'"
    )
    public void delete(Long umcProductMemberId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        saveUmcProductSquadParticipantPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductChapterMembershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductLeadershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberActivityPeriodPort.deleteAllByUmcProductMemberId(member.getId());
        manageFileUsageUseCase.removeAll(
            new BulkRemoveFileUsagesCommand(List.of(profileImageCoordinate(member.getId())))
        );
        saveUmcProductMemberPort.delete(member);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductMemberActivityPeriod",
        targetId = "#result",
        description = "'UMC PRODUCT 멤버 활동 기간을 생성했습니다.'"
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
        description = "'UMC PRODUCT 멤버 활동 기간을 수정했습니다.'"
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
        description = "'UMC PRODUCT 멤버 활동 기간을 삭제했습니다.'"
    )
    public void deleteActivityPeriod(Long umcProductMemberId, Long activityPeriodId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        UmcProductMemberActivityPeriod activityPeriod = loadUmcProductMemberActivityPeriodPort
            .getById(activityPeriodId);
        validateOwnedBy(activityPeriod, member.getId());
        if (loadUmcProductChapterMembershipPort.existsByMemberActivityPeriodId(activityPeriodId)
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
        targetType = "UmcProductChapterMembership",
        targetId = "#result",
        description = "'UMC PRODUCT Chapter 소속을 생성했습니다.'"
    )
    public Long createChapterMembership(CreateUmcProductChapterMembershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductChapter chapter = loadUmcProductChapterPort.getById(command.chapterId());
        validateChapterMembershipNotOverlapped(command, member.getId(), null);
        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            activityPeriod,
            chapter,
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        return saveUmcProductChapterMembershipPort.save(membership).getId();
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductChapterMembership",
        targetId = "#command.chapterMembershipId()",
        description = "'UMC PRODUCT Chapter 소속을 수정했습니다.'"
    )
    public void updateChapterMembership(UpdateUmcProductChapterMembershipCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductChapterMembership membership = loadUmcProductChapterMembershipPort
            .getById(command.chapterMembershipId());
        validateOwnedBy(membership, member.getId());
        validatePeriod(command.startDate(), command.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductChapter chapter = loadUmcProductChapterPort.getById(command.chapterId());
        validateChapterMembershipNotOverlapped(command, member.getId(), membership.getId());
        membership.update(
            activityPeriod,
            chapter,
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        saveUmcProductChapterMembershipPort.save(membership);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductChapterMembership",
        targetId = "#chapterMembershipId",
        description = "'UMC PRODUCT Chapter 소속을 삭제했습니다.'"
    )
    public void deleteChapterMembership(Long umcProductMemberId, Long chapterMembershipId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        UmcProductChapterMembership membership = loadUmcProductChapterMembershipPort.getById(chapterMembershipId);
        validateOwnedBy(membership, member.getId());
        saveUmcProductChapterMembershipPort.delete(membership);
    }

    @Override
    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductLeadership",
        targetId = "#result",
        description = "'UMC PRODUCT Leadership을 생성했습니다.'"
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
        description = "'UMC PRODUCT Leadership을 수정했습니다.'"
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
        description = "'UMC PRODUCT Leadership을 삭제했습니다.'"
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
        boolean outOfRange = loadUmcProductChapterMembershipPort.listByUmcProductMemberId(memberId).stream()
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

    private void validateChapterMembershipNotOverlapped(
        CreateUmcProductChapterMembershipCommand command,
        Long memberId,
        Long excludedChapterMembershipId
    ) {
        validateChapterMembershipNotOverlapped(
            memberId, command.chapterId(), command.startDate(), command.endDate(), excludedChapterMembershipId
        );
    }

    private void validateChapterMembershipNotOverlapped(
        UpdateUmcProductChapterMembershipCommand command,
        Long memberId,
        Long excludedChapterMembershipId
    ) {
        validateChapterMembershipNotOverlapped(
            memberId, command.chapterId(), command.startDate(), command.endDate(), excludedChapterMembershipId
        );
    }

    private void validateChapterMembershipNotOverlapped(
        Long memberId,
        Long chapterId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedChapterMembershipId
    ) {
        if (loadUmcProductChapterMembershipPort.existsOverlappingChapterMembership(
            memberId,
            chapterId,
            startDate,
            endDate,
            excludedChapterMembershipId
        )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED);
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

    private void validateOwnedBy(UmcProductChapterMembership membership, Long memberId) {
        if (!Objects.equals(membership.getMemberActivityPeriod().getUmcProductMember().getId(), memberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND);
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

    private void validateProfileImage(String profileImageId, Long requesterMemberId) {
        if (profileImageId != null && !profileImageId.isBlank()) {
            getFileUseCase.batchGetUsableByIds(List.of(profileImageId), requesterMemberId);
        }
    }

    private void replaceProfileImageUsage(UmcProductMember member, Long requesterMemberId) {
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            profileImageCoordinate(member.getId()),
            member.getProfileImageId() == null ? Set.of() : Set.of(member.getProfileImageId()),
            requesterMemberId
        ));
    }

    private FileUsageCoordinate profileImageCoordinate(Long umcProductMemberId) {
        return FileUsageCoordinate.of(
            "organization.umc-product-member",
            String.valueOf(umcProductMemberId),
            "profile-image"
        );
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

}
