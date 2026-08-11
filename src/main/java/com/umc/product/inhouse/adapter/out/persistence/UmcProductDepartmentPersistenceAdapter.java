package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductDepartmentPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductDepartmentPort;
import com.umc.product.inhouse.domain.UmcProductDepartment;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductDepartmentPersistenceAdapter implements LoadUmcProductDepartmentPort, SaveUmcProductDepartmentPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_department_code",
        InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_ALREADY_EXISTS,
        "fk_umc_product_department_parent",
        InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_HAS_CHILDREN
    );

    private final UmcProductDepartmentJpaRepository umcProductDepartmentJpaRepository;

    @Override
    public UmcProductDepartment getById(Long departmentId) {
        return umcProductDepartmentJpaRepository.findById(departmentId)
            .orElseThrow(() -> new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_NOT_FOUND));
    }

    @Override
    public UmcProductDepartment getByIdWithLock(Long departmentId) {
        return umcProductDepartmentJpaRepository.findByIdWithLock(departmentId)
            .orElseThrow(() -> new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_NOT_FOUND));
    }

    @Override
    public List<UmcProductDepartment> listAll(Boolean active, LocalDate activeOn) {
        return umcProductDepartmentJpaRepository.findAll(active, activeOn);
    }

    @Override
    public List<UmcProductDepartment> listAllWithLock() {
        return umcProductDepartmentJpaRepository.findAllWithLock();
    }

    @Override
    public List<UmcProductDepartment> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return umcProductDepartmentJpaRepository.findByIdIn(ids);
    }

    @Override
    public boolean existsByCode(String code, Long excludedDepartmentId) {
        return umcProductDepartmentJpaRepository.existsByCode(code, excludedDepartmentId);
    }

    @Override
    public boolean existsByParentId(Long parentDepartmentId) {
        return umcProductDepartmentJpaRepository.existsByParentId(parentDepartmentId);
    }

    @Override
    public UmcProductDepartment save(UmcProductDepartment department) {
        try {
            return umcProductDepartmentJpaRepository.saveAndFlush(department);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductDepartment department) {
        umcProductDepartmentJpaRepository.delete(department);
    }
}
