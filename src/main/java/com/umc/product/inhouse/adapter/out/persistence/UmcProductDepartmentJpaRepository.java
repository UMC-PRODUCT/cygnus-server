package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductDepartment;

import jakarta.persistence.LockModeType;

public interface UmcProductDepartmentJpaRepository extends JpaRepository<UmcProductDepartment, Long> {

    List<UmcProductDepartment> findByIdIn(Collection<Long> ids);

    @Query("""
        SELECT COUNT(s) > 0
        FROM UmcProductDepartment s
        WHERE s.code = :code
          AND (:excludedDepartmentId IS NULL OR s.id <> :excludedDepartmentId)
        """)
    boolean existsByCode(
        @Param("code") String code,
        @Param("excludedDepartmentId") Long excludedDepartmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UmcProductDepartment s WHERE s.id = :departmentId")
    java.util.Optional<UmcProductDepartment> findByIdWithLock(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT s
        FROM UmcProductDepartment s
        WHERE (:active IS NULL OR s.isActive = :active)
          AND (CAST(:activeOn AS LocalDate) IS NULL OR (
            s.period.startDate <= :activeOn
            AND (s.period.endDate IS NULL OR s.period.endDate >= :activeOn)
          ))
        ORDER BY s.sortOrder ASC, s.id ASC
        """)
    List<UmcProductDepartment> findAll(
        @Param("active") Boolean active,
        @Param("activeOn") LocalDate activeOn
    );
}
