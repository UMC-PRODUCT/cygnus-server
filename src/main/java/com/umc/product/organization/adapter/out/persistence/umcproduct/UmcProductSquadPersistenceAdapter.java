package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveUmcProductSquadPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductSquadPersistenceAdapter implements LoadUmcProductSquadPort, SaveUmcProductSquadPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_squad_code",
        OrganizationErrorCode.UMC_PRODUCT_SQUAD_ALREADY_EXISTS
    );

    private final UmcProductSquadJpaRepository umcProductSquadJpaRepository;

    @Override
    public UmcProductSquad getById(Long squadId) {
        return umcProductSquadJpaRepository.findById(squadId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_NOT_FOUND));
    }

    @Override
    public UmcProductSquad getByIdWithLock(Long squadId) {
        return umcProductSquadJpaRepository.findByIdWithLock(squadId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_NOT_FOUND));
    }

    @Override
    public List<UmcProductSquad> listAll(Boolean active, LocalDate activeOn) {
        return umcProductSquadJpaRepository.findAll(active, activeOn);
    }

    @Override
    public List<UmcProductSquad> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return umcProductSquadJpaRepository.findByIdIn(ids);
    }

    @Override
    public boolean existsByCode(String code, Long excludedSquadId) {
        return umcProductSquadJpaRepository.existsByCode(code, excludedSquadId);
    }

    @Override
    public UmcProductSquad save(UmcProductSquad squad) {
        try {
            return umcProductSquadJpaRepository.saveAndFlush(squad);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductSquad squad) {
        umcProductSquadJpaRepository.delete(squad);
    }
}
