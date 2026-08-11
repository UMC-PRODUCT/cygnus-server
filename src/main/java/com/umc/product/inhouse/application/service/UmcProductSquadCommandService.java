package com.umc.product.inhouse.application.service;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductSquadUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadParticipantCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadParticipantCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductSquadPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.domain.UmcProductSquad;
import com.umc.product.inhouse.domain.UmcProductSquadParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductSquadCommandService implements ManageUmcProductSquadUseCase {

    private final LoadUmcProductSquadPort loadUmcProductSquadPort;
    private final SaveUmcProductSquadPort saveUmcProductSquadPort;
    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;
    private final LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;
    private final SaveUmcProductSquadParticipantPort saveUmcProductSquadParticipantPort;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductSquad",
        targetId = "#result",
        description = "'UMC PRODUCT 스쿼드를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductSquadCommand command) {
        validateCanManage(command.requesterMemberId());
        validateCodeNotDuplicated(command.code(), null);
        UmcProductSquad squad = UmcProductSquad.create(
            command.code(),
            command.name(),
            command.description(),
            command.startDate(),
            command.endDate(),
            command.sortOrder(),
            command.active()
        );
        return saveUmcProductSquadPort.save(squad).getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductSquad",
        targetId = "#command.squadId()",
        description = "'UMC PRODUCT 스쿼드를 수정했습니다.'"
    )
    @Override
    public void update(UpdateUmcProductSquadCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductSquad squad = loadUmcProductSquadPort.getByIdWithLock(command.squadId());
        if (command.code() != null) {
            validateCodeNotDuplicated(command.code(), squad.getId());
        }
        LocalDate nextStartDate = command.startDate() != null ? command.startDate() : squad.getStartDate();
        validatePeriod(nextStartDate, command.endDate());
        validateParticipantsContained(squad.getId(), nextStartDate, command.endDate());
        squad.update(
            command.code(),
            command.name(),
            command.description(),
            nextStartDate,
            command.endDate(),
            command.sortOrder(),
            command.active()
        );
        saveUmcProductSquadPort.save(squad);
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductSquad",
        targetId = "#squadId",
        description = "'UMC PRODUCT 스쿼드를 삭제했습니다.'"
    )
    @Override
    public void delete(Long squadId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductSquad squad = loadUmcProductSquadPort.getByIdWithLock(squadId);
        if (loadUmcProductSquadParticipantPort.existsBySquadId(squadId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_HAS_PARTICIPANTS);
        }
        saveUmcProductSquadPort.delete(squad);
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductSquadParticipant",
        targetId = "#result",
        description = "'UMC PRODUCT 스쿼드 참여 이력을 생성했습니다.'"
    )
    @Override
    public Long createParticipant(CreateUmcProductSquadParticipantCommand command) {
        validateCanManage(command.requesterMemberId());
        validatePeriod(command.startDate(), command.endDate());

        UmcProductMember member = loadUmcProductMemberPort.getByIdWithLock(command.umcProductMemberId());
        UmcProductSquad squad = loadUmcProductSquadPort.getByIdWithLock(command.squadId());
        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            member.getId(), command.startDate(), command.endDate()
        );
        UmcProductSquadParticipant participant = UmcProductSquadParticipant.create(
            squad,
            activityPeriod,
            command.role(),
            command.position(),
            command.responsibilityTitle(),
            command.responsibilityDescription(),
            command.startDate(),
            command.endDate()
        );
        validateParticipationNotOverlapped(
            squad.getId(),
            member.getId(),
            participant.getRole(),
            participant.getStartDate(),
            participant.getEndDate(),
            null
        );
        return saveUmcProductSquadParticipantPort.save(participant).getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductSquadParticipant",
        targetId = "#command.participantId()",
        description = "'UMC PRODUCT 스쿼드 참여 이력을 수정했습니다.'"
    )
    @Override
    public void updateParticipant(UpdateUmcProductSquadParticipantCommand command) {
        validateCanManage(command.requesterMemberId());
        validatePeriod(command.startDate(), command.endDate());

        UmcProductSquadParticipant participant = loadUmcProductSquadParticipantPort.getById(command.participantId());
        Long memberId = participant.getMemberActivityPeriod().getUmcProductMember().getId();
        loadUmcProductMemberPort.getByIdWithLock(memberId);
        UmcProductSquad squad = loadUmcProductSquadPort.getByIdWithLock(command.squadId());
        validateBelongsToSquad(participant, squad.getId());

        UmcProductMemberActivityPeriod activityPeriod = getContainingPeriod(
            memberId, command.startDate(), command.endDate()
        );
        validateParticipationNotOverlapped(
            squad.getId(),
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
        saveUmcProductSquadParticipantPort.save(participant);
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductSquadParticipant",
        targetId = "#participantId",
        description = "'UMC PRODUCT 스쿼드 참여 이력을 삭제했습니다.'"
    )
    @Override
    public void deleteParticipant(Long squadId, Long participantId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductSquadParticipant participant = loadUmcProductSquadParticipantPort.getById(participantId);
        Long memberId = participant.getMemberActivityPeriod().getUmcProductMember().getId();
        loadUmcProductMemberPort.getByIdWithLock(memberId);
        UmcProductSquad squad = loadUmcProductSquadPort.getByIdWithLock(squadId);
        validateBelongsToSquad(participant, squad.getId());
        saveUmcProductSquadParticipantPort.delete(participant);
    }

    private void validateParticipantsContained(Long squadId, LocalDate startDate, LocalDate endDate) {
        boolean outOfRange = loadUmcProductSquadParticipantPort.listBySquadId(squadId).stream()
            .anyMatch(participant -> !contains(
                startDate,
                endDate,
                participant.getStartDate(),
                participant.getEndDate()
            ));
        if (outOfRange) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }

    private void validateParticipationNotOverlapped(
        Long squadId,
        Long memberId,
        UmcProductSquadRole role,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedParticipantId
    ) {
        if (loadUmcProductSquadParticipantPort.existsOverlappingMemberInSquad(
            squadId, memberId, startDate, endDate, excludedParticipantId
        )) {
            throw new OrganizationDomainException(
                OrganizationErrorCode.UMC_PRODUCT_SQUAD_PARTICIPATION_OVERLAPPED
            );
        }
        if (role == UmcProductSquadRole.SQUAD_LEAD
            && loadUmcProductSquadParticipantPort.existsOverlappingSquadLead(
                squadId, startDate, endDate, excludedParticipantId
            )) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED);
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

    private void validateBelongsToSquad(UmcProductSquadParticipant participant, Long squadId) {
        if (!Objects.equals(participant.getSquad().getId(), squadId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }

    private void validateCodeNotDuplicated(String code, Long excludedSquadId) {
        if (code != null && loadUmcProductSquadPort.existsByCode(code.trim(), excludedSquadId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_ALREADY_EXISTS);
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
}
