package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentParticipantPort;
import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductDepartmentParticipantPersistenceAdapter
    implements LoadUmcProductDepartmentParticipantPort, SaveUmcProductDepartmentParticipantPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_department_participant_member_dates",
        InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPATION_OVERLAPPED,
        "ex_umc_product_department_lead_dates",
        InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_LEAD_OVERLAPPED
    );

    private final UmcProductDepartmentParticipantJpaRepository umcProductDepartmentParticipantJpaRepository;

    @Override
    public UmcProductDepartmentParticipant getById(Long departmentParticipantId) {
        return umcProductDepartmentParticipantJpaRepository.findById(departmentParticipantId)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_PARTICIPANT_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductDepartmentParticipant> listByDepartmentId(Long departmentId) {
        return umcProductDepartmentParticipantJpaRepository.findAllByDepartmentId(departmentId);
    }

    @Override
    public List<UmcProductDepartmentParticipant> listByUmcProductMemberId(Long umcProductMemberId) {
        return umcProductDepartmentParticipantJpaRepository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductDepartmentParticipant> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return umcProductDepartmentParticipantJpaRepository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public boolean existsByDepartmentId(Long departmentId) {
        return umcProductDepartmentParticipantJpaRepository.existsByDepartmentId(departmentId);
    }

    @Override
    public boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId) {
        return umcProductDepartmentParticipantJpaRepository.existsByMemberActivityPeriodId(memberActivityPeriodId);
    }

    @Override
    public boolean existsOverlappingMemberInDepartment(
        Long departmentId,
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedDepartmentParticipantId
    ) {
        return umcProductDepartmentParticipantJpaRepository.existsOverlappingMemberInDepartment(
            departmentId,
            umcProductMemberId,
            startDate,
            endDate,
            excludedDepartmentParticipantId
        );
    }

    @Override
    public boolean existsOverlappingDepartmentLead(
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedDepartmentParticipantId
    ) {
        return umcProductDepartmentParticipantJpaRepository.existsOverlappingDepartmentLead(
            departmentId,
            UmcProductDepartmentRole.DEPARTMENT_LEAD,
            startDate,
            endDate,
            excludedDepartmentParticipantId
        );
    }

    @Override
    public UmcProductDepartmentParticipant save(UmcProductDepartmentParticipant participant) {
        try {
            return umcProductDepartmentParticipantJpaRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductDepartmentParticipant participant) {
        umcProductDepartmentParticipantJpaRepository.delete(participant);
    }

    @Override
    public void deleteAllByDepartmentId(Long departmentId) {
        umcProductDepartmentParticipantJpaRepository.deleteAllByDepartmentId(departmentId);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        umcProductDepartmentParticipantJpaRepository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
