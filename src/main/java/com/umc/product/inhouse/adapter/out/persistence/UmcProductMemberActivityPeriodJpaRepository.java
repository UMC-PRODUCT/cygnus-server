package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductMemberActivityPeriod;

public interface UmcProductMemberActivityPeriodJpaRepository
    extends JpaRepository<UmcProductMemberActivityPeriod, Long> {

    @Query("""
        SELECT p
        FROM UmcProductMemberActivityPeriod p
        JOIN FETCH p.umcProductMember
        WHERE p.umcProductMember.id = :umcProductMemberId
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductMemberActivityPeriod> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT p
        FROM UmcProductMemberActivityPeriod p
        JOIN FETCH p.umcProductMember
        WHERE p.umcProductMember.id IN :umcProductMemberIds
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductMemberActivityPeriod> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    @Query("""
        SELECT p
        FROM UmcProductMemberActivityPeriod p
        WHERE p.umcProductMember.id = :umcProductMemberId
          AND p.period.startDate <= :startDate
          AND ((CAST(:endDate AS LocalDate) IS NULL AND p.period.endDate IS NULL)
            OR (CAST(:endDate AS LocalDate) IS NOT NULL
              AND (p.period.endDate IS NULL OR p.period.endDate >= :endDate)))
        """)
    Optional<UmcProductMemberActivityPeriod> findContaining(
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM umc_product_member_activity_period p
            WHERE p.umc_product_member_id = :umcProductMemberId
              AND (:excludedActivityPeriodId IS NULL OR p.id <> :excludedActivityPeriodId)
              AND p.start_date <= COALESCE(CAST(:endDate AS date), 'infinity'::date) + 1
              AND COALESCE(p.end_date, 'infinity'::date) >= CAST(:startDate AS date) - 1
        )
        """, nativeQuery = true)
    boolean existsOverlappingOrAdjacent(
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedActivityPeriodId") Long excludedActivityPeriodId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM UmcProductMemberActivityPeriod p
        WHERE p.umcProductMember.id = :umcProductMemberId
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
