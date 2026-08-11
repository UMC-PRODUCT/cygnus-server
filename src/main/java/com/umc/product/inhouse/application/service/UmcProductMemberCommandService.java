package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByMemberIdCommand;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductDepartmentUseCase;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductMemberUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.LinkUmcProductMemberAccountCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberCommand;
import com.umc.product.inhouse.application.port.in.command.dto.RegisterUmcProductMemberResult;
import com.umc.product.inhouse.application.port.in.command.dto.ResetUmcProductAccountPasswordResult;
import com.umc.product.inhouse.application.port.in.command.dto.UmcProductActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductChapterMembershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductLeadershipCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberActivityPeriodCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductMemberProfileCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductResponsibilitiesCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.UmcProductLeadership;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.application.port.in.command.ProvisionMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductMemberCommandService implements ManageUmcProductMemberUseCase {

    private static final String PROVISION_EMAIL_DOMAIN = "university.neordinary.com";
    private static final Pattern ENGLISH_NICKNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{2,30}$");

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final SaveUmcProductMemberPort saveUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final SaveUmcProductMemberActivityPeriodPort saveUmcProductMemberActivityPeriodPort;
    private final SaveUmcProductMemberAccountPort saveUmcProductMemberAccountPort;
    private final LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    private final SaveUmcProductChapterMembershipPort saveUmcProductChapterMembershipPort;
    private final LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;
    private final SaveUmcProductLeadershipPort saveUmcProductLeadershipPort;
    private final LoadUmcProductDepartmentParticipantPort loadUmcProductDepartmentParticipantPort;
    private final SaveUmcProductDepartmentParticipantPort saveUmcProductDepartmentParticipantPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetFileUseCase getFileUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final ProvisionMemberUseCase provisionMemberUseCase;
    private final CredentialAuthenticationUseCase credentialAuthenticationUseCase;
    private final ManageUmcProductDepartmentUseCase manageUmcProductDepartmentUseCase;
    private final UmcProductTempPasswordGenerator tempPasswordGenerator;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.CREATE,
        targetType = "UmcProductMember",
        targetId = "#result",
        description = "'UMC PRODUCT 멤버를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductMemberCommand command) {
        validateCanManage(command.requesterMemberId());
        validateSchool(command.schoolId());
        validateProfileImage(command.profileImageId());
        validateInitialActivityPeriods(command.activityPeriods());

        UmcProductMember member = saveUmcProductMemberPort.save(UmcProductMember.create(
            command.name(),
            command.nickname(),
            command.schoolId(),
            command.introduction(),
            command.profileImageId()
        ));
        command.activityPeriods().stream()
            .map(period -> UmcProductMemberActivityPeriod.create(member, period.startDate(), period.endDate()))
            .forEach(saveUmcProductMemberActivityPeriodPort::save);
        return member.getId();
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.CREATE,
        targetType = "UmcProductMember",
        targetId = "#result.umcProductMemberId()",
        description = "'UMC PRODUCT 인원을 등록하고 계정을 발급했습니다.'"
    )
    @Override
    public RegisterUmcProductMemberResult register(RegisterUmcProductMemberCommand command) {
        validateCanManage(command.requesterMemberId());
        validateEnglishNickname(command.englishNickname());
        String email = command.englishNickname() + "@" + PROVISION_EMAIL_DOMAIN;
        if (getMemberUseCase.existsByEmail(email)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_EMAIL_ALREADY_EXISTS);
        }
        validateSchool(command.schoolId());
        validateProfileImage(command.profileImageId());
        validateInitialActivityPeriods(command.activityPeriods());

        String temporaryPassword = tempPasswordGenerator.generate();
        Long memberId = provisionMemberUseCase.provision(new ProvisionMemberCommand(
            command.name(),
            command.nickname(),
            email,
            command.schoolId(),
            temporaryPassword
        ));
        UmcProductMember member = saveUmcProductMemberPort.save(UmcProductMember.create(
            command.name(),
            command.nickname(),
            command.schoolId(),
            command.introduction(),
            command.profileImageId()
        ));
        saveUmcProductMemberAccountPort.save(UmcProductMemberAccount.create(
            member,
            memberId,
            UmcProductMemberAccountType.PROVISIONED
        ));
        command.activityPeriods().stream()
            .map(period -> UmcProductMemberActivityPeriod.create(member, period.startDate(), period.endDate()))
            .forEach(saveUmcProductMemberActivityPeriodPort::save);
        command.chapterMemberships().forEach(seed -> createInitialChapterMembership(member, seed));
        command.departmentParticipations().forEach(seed ->
            manageUmcProductDepartmentUseCase.createParticipant(seed.toCreateCommand(
                command.requesterMemberId(),
                member.getId()
            ))
        );
        command.productLeaderships().forEach(seed -> createInitialLeadership(member, seed));

        return new RegisterUmcProductMemberResult(member.getId(), memberId, email, temporaryPassword);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.CREATE,
        targetType = "UmcProductMemberAccount",
        targetId = "#result",
        description = "'UMC PRODUCT 인원에 로그인 계정을 연동했습니다.'"
    )
    @Override
    public Long linkAccount(LinkUmcProductMemberAccountCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        getMemberUseCase.getById(command.memberId());
        if (loadUmcProductMemberAccountPort.existsByMemberId(command.memberId())) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_ALREADY_LINKED);
        }
        return saveUmcProductMemberAccountPort.save(UmcProductMemberAccount.create(
            member,
            command.memberId(),
            UmcProductMemberAccountType.LINKED
        )).getId();
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.DELETE,
        targetType = "UmcProductMemberAccount",
        targetId = "#memberId",
        description = "'UMC PRODUCT 인원의 로그인 계정 연동을 해제했습니다.'"
    )
    @Override
    public void unlinkAccount(Long umcProductMemberId, Long memberId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMemberAccount account = getAccount(umcProductMemberId, memberId);
        saveUmcProductMemberAccountPort.delete(account);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMemberAccount",
        targetId = "#memberId",
        description = "'UMC PRODUCT 발급 계정의 임시 비밀번호를 재발급했습니다.'"
    )
    @Override
    public ResetUmcProductAccountPasswordResult resetAccountPassword(
        Long umcProductMemberId,
        Long memberId,
        Long requesterMemberId
    ) {
        validateCanManage(requesterMemberId);
        UmcProductMemberAccount account = getAccount(umcProductMemberId, memberId);
        if (!account.isProvisioned()) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_NOT_RESETTABLE);
        }
        String temporaryPassword = tempPasswordGenerator.generate();
        String email = getMemberUseCase.getById(memberId).email();
        credentialAuthenticationUseCase.resetPasswordByMemberId(
            new ResetPasswordByMemberIdCommand(memberId, temporaryPassword)
        );
        return new ResetUmcProductAccountPasswordResult(email, temporaryPassword);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMember",
        targetId = "#command.umcProductMemberId()",
        description = "'UMC PRODUCT 멤버 프로필을 수정했습니다.'"
    )
    @Override
    public void updateProfile(UpdateUmcProductMemberProfileCommand command) {
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        if (!umcProductAccessPolicy.canManageMemberProfile(command.requesterMemberId(), member.getId())) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
        validateProfileImage(command.profileImageId());
        member.updateProfile(command.introduction(), command.profileImageId());
        saveUmcProductMemberPort.save(member);
    }

    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.UPDATE,
        targetType = "UmcProductMemberResponsibilities",
        targetId = "#command.umcProductMemberId()",
        description = "'UMC PRODUCT 인원의 하는 일을 수정했습니다.'"
    )
    @Override
    public void updateResponsibilities(UpdateUmcProductResponsibilitiesCommand command) {
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        if (!umcProductAccessPolicy.canManageMemberProfile(command.requesterMemberId(), member.getId())) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
        validateResponsibilityIdsNotDuplicated(command);

        List<UmcProductChapterMembership> memberships = new ArrayList<>(command.chapterMemberships().size());
        for (UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility update : command.chapterMemberships()) {
            UmcProductChapterMembership membership = loadUmcProductChapterMembershipPort
                .getById(update.chapterMembershipId());
            validateOwnedBy(membership, member.getId());
            memberships.add(membership);
        }
        List<UmcProductDepartmentParticipant> participants =
            new ArrayList<>(command.departmentParticipations().size());
        for (UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility update
            : command.departmentParticipations()) {
            UmcProductDepartmentParticipant participant =
                loadUmcProductDepartmentParticipantPort.getById(update.departmentParticipantId());
            validateOwnedBy(participant, member.getId());
            participants.add(participant);
        }

        for (int index = 0; index < memberships.size(); index++) {
            UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility update =
                command.chapterMemberships().get(index);
            UmcProductChapterMembership membership = memberships.get(index);
            membership.updateResponsibility(update.responsibilityTitle(), update.responsibilityDescription());
            saveUmcProductChapterMembershipPort.save(membership);
        }
        for (int index = 0; index < participants.size(); index++) {
            UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility update =
                command.departmentParticipations().get(index);
            UmcProductDepartmentParticipant participant = participants.get(index);
            participant.updateResponsibility(update.responsibilityTitle(), update.responsibilityDescription());
            saveUmcProductDepartmentParticipantPort.save(participant);
        }
    }

    @Override
    @Audited(
        domain = Domain.INHOUSE,
        action = AuditAction.DELETE,
        targetType = "UmcProductMember",
        targetId = "#umcProductMemberId",
        description = "'UMC PRODUCT 멤버를 삭제했습니다.'"
    )
    public void delete(Long umcProductMemberId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(umcProductMemberId);
        saveUmcProductDepartmentParticipantPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductChapterMembershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductLeadershipPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberActivityPeriodPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberAccountPort.deleteAllByUmcProductMemberId(member.getId());
        saveUmcProductMemberPort.delete(member);
    }

    @Override
    @Audited(
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
            || loadUmcProductDepartmentParticipantPort.existsByMemberActivityPeriodId(activityPeriodId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS);
        }
        saveUmcProductMemberActivityPeriodPort.delete(activityPeriod);
    }

    @Override
    @Audited(
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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
        domain = Domain.INHOUSE,
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

    private void createInitialChapterMembership(
        UmcProductMember member,
        RegisterUmcProductChapterMembershipCommand seed
    ) {
        validatePeriod(seed.startDate(), seed.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), seed.startDate(), seed.endDate()
        );
        UmcProductChapter chapter = loadUmcProductChapterPort.getById(seed.chapterId());
        validateChapterMembershipNotOverlapped(
            member.getId(), seed.chapterId(), seed.startDate(), seed.endDate(), null
        );
        saveUmcProductChapterMembershipPort.save(UmcProductChapterMembership.create(
            activityPeriod,
            chapter,
            seed.position(),
            seed.responsibilityTitle(),
            seed.responsibilityDescription(),
            seed.startDate(),
            seed.endDate()
        ));
    }

    private void createInitialLeadership(
        UmcProductMember member,
        RegisterUmcProductLeadershipCommand seed
    ) {
        validatePeriod(seed.startDate(), seed.endDate());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), seed.startDate(), seed.endDate()
        );
        validateLeadershipNotOverlapped(
            member.getId(), seed.role(), seed.startDate(), seed.endDate(), null
        );
        saveUmcProductLeadershipPort.save(UmcProductLeadership.create(
            activityPeriod, seed.role(), seed.startDate(), seed.endDate()
        ));
    }

    private void validateEnglishNickname(String englishNickname) {
        if (englishNickname == null || !ENGLISH_NICKNAME_PATTERN.matcher(englishNickname).matches()) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ENGLISH_NICKNAME_INVALID);
        }
    }

    private void validateInitialActivityPeriods(List<UmcProductActivityPeriodCommand> periods) {
        if (periods == null || periods.isEmpty()) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        }
        periods.forEach(period -> validatePeriod(period.startDate(), period.endDate()));
        List<UmcProductActivityPeriodCommand> sorted = periods.stream()
            .sorted(Comparator.comparing(UmcProductActivityPeriodCommand::startDate))
            .toList();
        for (int index = 1; index < sorted.size(); index++) {
            LocalDate previousEnd = sorted.get(index - 1).endDate();
            LocalDate nextStart = sorted.get(index).startDate();
            if (previousEnd == null || !nextStart.isAfter(nextDay(previousEnd))) {
                throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
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
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED);
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
            || loadUmcProductDepartmentParticipantPort.listByUmcProductMemberId(memberId).stream()
            .filter(item -> Objects.equals(item.getMemberActivityPeriod().getId(), activityPeriod.getId()))
            .anyMatch(item -> !contains(startDate, endDate, item.getStartDate(), item.getEndDate()));
        if (outOfRange) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
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
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED);
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
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_LEADERSHIP_OVERLAPPED);
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

    private void validateOwnedBy(UmcProductMemberActivityPeriod activityPeriod, Long memberId) {
        if (!Objects.equals(activityPeriod.getUmcProductMember().getId(), memberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND);
        }
    }

    private void validateOwnedBy(UmcProductChapterMembership membership, Long memberId) {
        if (!Objects.equals(membership.getMemberActivityPeriod().getUmcProductMember().getId(), memberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND);
        }
    }

    private void validateOwnedBy(UmcProductLeadership leadership, Long memberId) {
        if (!Objects.equals(leadership.getMemberActivityPeriod().getUmcProductMember().getId(), memberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_LEADERSHIP_NOT_FOUND);
        }
    }

    private void validateOwnedBy(UmcProductDepartmentParticipant participant, Long memberId) {
        if (!Objects.equals(
            participant.getMemberActivityPeriod().getUmcProductMember().getId(),
            memberId
        )) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateResponsibilityIdsNotDuplicated(UpdateUmcProductResponsibilitiesCommand command) {
        Set<Long> chapterMembershipIds = new HashSet<>();
        boolean chapterDuplicated = command.chapterMemberships().stream()
            .map(UpdateUmcProductResponsibilitiesCommand.ChapterResponsibility::chapterMembershipId)
            .anyMatch(id -> !chapterMembershipIds.add(id));
        Set<Long> departmentParticipantIds = new HashSet<>();
        boolean departmentDuplicated = command.departmentParticipations().stream()
            .map(UpdateUmcProductResponsibilitiesCommand.DepartmentResponsibility::departmentParticipantId)
            .anyMatch(id -> !departmentParticipantIds.add(id));
        if (chapterDuplicated || departmentDuplicated) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_RESPONSIBILITY_DUPLICATED);
        }
    }

    private UmcProductMemberAccount getAccount(Long umcProductMemberId, Long memberId) {
        UmcProductMemberAccount account = loadUmcProductMemberAccountPort.findByMemberId(memberId)
            .orElseThrow(() -> new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_NOT_FOUND));
        if (!Objects.equals(account.getUmcProductMember().getId(), umcProductMemberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_NOT_FOUND);
        }
        return account;
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }

    private void validateSchool(Long schoolId) {
        if (schoolId != null) {
            getSchoolUseCase.getSchoolDetail(schoolId);
        }
    }

    private void validateProfileImage(String profileImageId) {
        if (profileImageId != null && !profileImageId.isBlank() && !getFileUseCase.existsById(profileImageId)) {
            throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
        }
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

    private static LocalDate nextDay(LocalDate date) {
        return date.equals(LocalDate.MAX) ? LocalDate.MAX : date.plusDays(1);
    }

}
