package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductDepartmentParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;

public interface UmcProductDepartmentParticipantJpaRepository
    extends JpaRepository<UmcProductDepartmentParticipant, Long> {

    @Query("""
        SELECT p
        FROM UmcProductDepartmentParticipant p
        JOIN FETCH p.department
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE p.department.id = :departmentId
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductDepartmentParticipant> findAllByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT p
        FROM UmcProductDepartmentParticipant p
        JOIN FETCH p.department
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE period.umcProductMember.id = :umcProductMemberId
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductDepartmentParticipant> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT p
        FROM UmcProductDepartmentParticipant p
        JOIN FETCH p.department
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE period.umcProductMember.id IN :umcProductMemberIds
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductDepartmentParticipant> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    @Query("""
        SELECT COUNT(p) > 0
        FROM UmcProductDepartmentParticipant p
        WHERE p.department.id = :departmentId
          AND p.memberActivityPeriod.umcProductMember.id = :umcProductMemberId
          AND (:excludedDepartmentParticipantId IS NULL OR p.id <> :excludedDepartmentParticipantId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR p.period.startDate <= :endDate)
          AND (p.period.endDate IS NULL OR p.period.endDate >= :startDate)
        """)
    boolean existsOverlappingMemberInDepartment(
        @Param("departmentId") Long departmentId,
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedDepartmentParticipantId") Long excludedDepartmentParticipantId
    );

    @Query("""
        SELECT COUNT(p) > 0
        FROM UmcProductDepartmentParticipant p
        WHERE p.department.id = :departmentId
          AND p.role = :role
          AND (:excludedDepartmentParticipantId IS NULL OR p.id <> :excludedDepartmentParticipantId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR p.period.startDate <= :endDate)
          AND (p.period.endDate IS NULL OR p.period.endDate >= :startDate)
        """)
    boolean existsOverlappingDepartmentLead(
        @Param("departmentId") Long departmentId,
        @Param("role") UmcProductDepartmentRole role,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedDepartmentParticipantId") Long excludedDepartmentParticipantId
    );

    @Modifying
    @Query("DELETE FROM UmcProductDepartmentParticipant p WHERE p.department.id = :departmentId")
    void deleteAllByDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("""
        DELETE FROM UmcProductDepartmentParticipant p
        WHERE p.memberActivityPeriod.id IN (
            SELECT period.id
            FROM UmcProductMemberActivityPeriod period
            WHERE period.umcProductMember.id = :umcProductMemberId
        )
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
