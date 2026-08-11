package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductSquadParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.inhouse.domain.UmcProductSquadParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductSquadParticipantPersistenceAdapter
    implements LoadUmcProductSquadParticipantPort, SaveUmcProductSquadParticipantPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_squad_participant_member_dates",
        InhouseErrorCode.UMC_PRODUCT_SQUAD_PARTICIPATION_OVERLAPPED,
        "ex_umc_product_squad_lead_dates",
        InhouseErrorCode.UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED
    );

    private final UmcProductSquadParticipantJpaRepository umcProductSquadParticipantJpaRepository;

    @Override
    public UmcProductSquadParticipant getById(Long squadParticipantId) {
        return umcProductSquadParticipantJpaRepository.findById(squadParticipantId)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductSquadParticipant> listBySquadId(Long squadId) {
        return umcProductSquadParticipantJpaRepository.findAllBySquadId(squadId);
    }

    @Override
    public List<UmcProductSquadParticipant> listByUmcProductMemberId(Long umcProductMemberId) {
        return umcProductSquadParticipantJpaRepository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductSquadParticipant> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return umcProductSquadParticipantJpaRepository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public boolean existsBySquadId(Long squadId) {
        return umcProductSquadParticipantJpaRepository.existsBySquadId(squadId);
    }

    @Override
    public boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId) {
        return umcProductSquadParticipantJpaRepository.existsByMemberActivityPeriodId(memberActivityPeriodId);
    }

    @Override
    public boolean existsOverlappingMemberInSquad(
        Long squadId,
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedSquadParticipantId
    ) {
        return umcProductSquadParticipantJpaRepository.existsOverlappingMemberInSquad(
            squadId,
            umcProductMemberId,
            startDate,
            endDate,
            excludedSquadParticipantId
        );
    }

    @Override
    public boolean existsOverlappingSquadLead(
        Long squadId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedSquadParticipantId
    ) {
        return umcProductSquadParticipantJpaRepository.existsOverlappingSquadLead(
            squadId,
            UmcProductSquadRole.SQUAD_LEAD,
            startDate,
            endDate,
            excludedSquadParticipantId
        );
    }

    @Override
    public UmcProductSquadParticipant save(UmcProductSquadParticipant participant) {
        try {
            return umcProductSquadParticipantJpaRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductSquadParticipant participant) {
        umcProductSquadParticipantJpaRepository.delete(participant);
    }

    @Override
    public void deleteAllBySquadId(Long squadId) {
        umcProductSquadParticipantJpaRepository.deleteAllBySquadId(squadId);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        umcProductSquadParticipantJpaRepository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
