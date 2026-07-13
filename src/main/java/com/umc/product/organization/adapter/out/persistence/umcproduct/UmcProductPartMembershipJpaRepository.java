package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.UmcProductPartMembership;
import com.umc.product.organization.domain.enums.UmcProductPartRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

public interface UmcProductPartMembershipJpaRepository extends JpaRepository<UmcProductPartMembership, Long> {

    @Query("""
        SELECT m
        FROM UmcProductPartMembership m
        JOIN FETCH m.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        JOIN FETCH m.part part
        JOIN FETCH part.chapter
        WHERE p.umcProductMember.id = :umcProductMemberId
        ORDER BY m.period.startDate DESC, m.id DESC
        """)
    List<UmcProductPartMembership> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT m
        FROM UmcProductPartMembership m
        JOIN FETCH m.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        JOIN FETCH m.part part
        JOIN FETCH part.chapter
        WHERE p.umcProductMember.id IN :umcProductMemberIds
        ORDER BY m.period.startDate DESC, m.id DESC
        """)
    List<UmcProductPartMembership> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    boolean existsByPartId(Long partId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    @Query("""
        SELECT COUNT(m) > 0
        FROM UmcProductPartMembership m
        WHERE m.memberActivityPeriod.umcProductMember.id = :umcProductMemberId
          AND m.part.id = :partId
          AND m.role = :role
          AND m.position = :position
          AND ((:responsibilityTitle IS NULL AND m.responsibilityTitle IS NULL)
            OR m.responsibilityTitle = :responsibilityTitle)
          AND (:excludedPartMembershipId IS NULL OR m.id <> :excludedPartMembershipId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR m.period.startDate <= :endDate)
          AND (m.period.endDate IS NULL OR m.period.endDate >= :startDate)
        """)
    boolean existsOverlappingSameAssignment(
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("partId") Long partId,
        @Param("role") UmcProductPartRole role,
        @Param("position") UmcProductPosition position,
        @Param("responsibilityTitle") String responsibilityTitle,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedPartMembershipId") Long excludedPartMembershipId
    );

    @Query("""
        SELECT COUNT(m) > 0
        FROM UmcProductPartMembership m
        WHERE m.part.id = :partId
          AND m.role = :role
          AND (:excludedPartMembershipId IS NULL OR m.id <> :excludedPartMembershipId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR m.period.startDate <= :endDate)
          AND (m.period.endDate IS NULL OR m.period.endDate >= :startDate)
        """)
    boolean existsOverlappingPartLead(
        @Param("partId") Long partId,
        @Param("role") UmcProductPartRole role,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedPartMembershipId") Long excludedPartMembershipId
    );

    @Modifying
    @Query("""
        DELETE FROM UmcProductPartMembership m
        WHERE m.memberActivityPeriod.id IN (
            SELECT p.id
            FROM UmcProductMemberActivityPeriod p
            WHERE p.umcProductMember.id = :umcProductMemberId
        )
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
