package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductDepartment;

public interface LoadUmcProductDepartmentPort {

    UmcProductDepartment getById(Long departmentId);

    UmcProductDepartment getByIdWithLock(Long departmentId);

    List<UmcProductDepartment> listAll(Boolean active, LocalDate activeOn);

    List<UmcProductDepartment> listAllWithLock();

    List<UmcProductDepartment> listByIds(Collection<Long> ids);

    boolean existsByCode(String code, Long excludedDepartmentId);

    boolean existsByParentId(Long parentDepartmentId);
}
