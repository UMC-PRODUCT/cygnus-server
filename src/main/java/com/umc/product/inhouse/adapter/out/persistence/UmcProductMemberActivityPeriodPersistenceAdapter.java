package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductMemberActivityPeriodPersistenceAdapter
    implements LoadUmcProductMemberActivityPeriodPort, SaveUmcProductMemberActivityPeriodPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_activity_period_member_dates",
        InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED
    );

    private final UmcProductMemberActivityPeriodJpaRepository repository;

    @Override
    public UmcProductMemberActivityPeriod getById(Long activityPeriodId) {
        return repository.findById(activityPeriodId)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductMemberActivityPeriod> listByUmcProductMemberId(Long umcProductMemberId) {
        return repository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductMemberActivityPeriod> listByUmcProductMemberIds(
        Collection<Long> umcProductMemberIds
    ) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public Optional<UmcProductMemberActivityPeriod> findContaining(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return repository.findContaining(umcProductMemberId, startDate, endDate);
    }

    @Override
    public boolean existsOverlappingOrAdjacent(
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedActivityPeriodId
    ) {
        return repository.existsOverlappingOrAdjacent(
            umcProductMemberId,
            startDate,
            endDate,
            excludedActivityPeriodId
        );
    }

    @Override
    public UmcProductMemberActivityPeriod save(UmcProductMemberActivityPeriod activityPeriod) {
        try {
            return repository.saveAndFlush(activityPeriod);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductMemberActivityPeriod activityPeriod) {
        repository.delete(activityPeriod);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        repository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
