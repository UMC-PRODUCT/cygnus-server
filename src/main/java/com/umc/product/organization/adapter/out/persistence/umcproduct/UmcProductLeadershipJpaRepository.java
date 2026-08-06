package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

public interface UmcProductLeadershipJpaRepository extends JpaRepository<UmcProductLeadership, Long> {

    @Query("""
        SELECT l
        FROM UmcProductLeadership l
        JOIN FETCH l.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        WHERE p.umcProductMember.id = :umcProductMemberId
        ORDER BY l.period.startDate DESC, l.id DESC
        """)
    List<UmcProductLeadership> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT l
        FROM UmcProductLeadership l
        JOIN FETCH l.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        WHERE p.umcProductMember.id IN :umcProductMemberIds
        ORDER BY l.period.startDate DESC, l.id DESC
        """)
    List<UmcProductLeadership> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    @Query("""
        SELECT COUNT(l) > 0
        FROM UmcProductLeadership l
        WHERE l.memberActivityPeriod.umcProductMember.memberId = :memberId
          AND l.role IN :roles
          AND l.period.startDate <= :activeOn
          AND (l.period.endDate IS NULL OR l.period.endDate >= :activeOn)
          AND l.memberActivityPeriod.period.startDate <= :activeOn
          AND (l.memberActivityPeriod.period.endDate IS NULL
            OR l.memberActivityPeriod.period.endDate >= :activeOn)
        """)
    boolean existsByMemberIdAndRolesOnDate(
        @Param("memberId") Long memberId,
        @Param("roles") Set<UmcProductLeadershipRole> roles,
        @Param("activeOn") LocalDate activeOn
    );

    @Query("""
        SELECT COUNT(l) > 0
        FROM UmcProductLeadership l
        WHERE l.role = :role
          AND (:excludedLeadershipId IS NULL OR l.id <> :excludedLeadershipId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR l.period.startDate <= :endDate)
          AND (l.period.endDate IS NULL OR l.period.endDate >= :startDate)
        """)
    boolean existsOverlappingRole(
        @Param("role") UmcProductLeadershipRole role,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedLeadershipId") Long excludedLeadershipId
    );

    @Query("""
        SELECT COUNT(l) > 0
        FROM UmcProductLeadership l
        WHERE l.memberActivityPeriod.umcProductMember.id = :umcProductMemberId
          AND (:excludedLeadershipId IS NULL OR l.id <> :excludedLeadershipId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR l.period.startDate <= :endDate)
          AND (l.period.endDate IS NULL OR l.period.endDate >= :startDate)
        """)
    boolean existsOverlappingMember(
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedLeadershipId") Long excludedLeadershipId
    );

    @Modifying
    @Query("""
        DELETE FROM UmcProductLeadership l
        WHERE l.memberActivityPeriod.id IN (
            SELECT p.id
            FROM UmcProductMemberActivityPeriod p
            WHERE p.umcProductMember.id = :umcProductMemberId
        )
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
