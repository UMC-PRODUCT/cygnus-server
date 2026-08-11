package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;

public interface LoadUmcProductDepartmentParticipantPort {

    UmcProductDepartmentParticipant getById(Long departmentParticipantId);

    List<UmcProductDepartmentParticipant> listByDepartmentId(Long departmentId);

    List<UmcProductDepartmentParticipant> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductDepartmentParticipant> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    boolean existsOverlappingMemberInDepartment(
        Long departmentId,
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedDepartmentParticipantId
    );

    boolean existsOverlappingDepartmentLead(
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedDepartmentParticipantId
    );
}
