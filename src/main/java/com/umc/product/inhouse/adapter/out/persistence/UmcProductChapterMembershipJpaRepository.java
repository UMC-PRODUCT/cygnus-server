package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductChapterMembership;

public interface UmcProductChapterMembershipJpaRepository extends JpaRepository<UmcProductChapterMembership, Long> {

    @Query("""
        SELECT m
        FROM UmcProductChapterMembership m
        JOIN FETCH m.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        JOIN FETCH m.chapter
        WHERE p.umcProductMember.id = :umcProductMemberId
        ORDER BY m.period.startDate DESC, m.id DESC
        """)
    List<UmcProductChapterMembership> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT m
        FROM UmcProductChapterMembership m
        JOIN FETCH m.memberActivityPeriod p
        JOIN FETCH p.umcProductMember
        JOIN FETCH m.chapter
        WHERE p.umcProductMember.id IN :umcProductMemberIds
        ORDER BY m.period.startDate DESC, m.id DESC
        """)
    List<UmcProductChapterMembership> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    boolean existsByChapterId(Long chapterId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    @Query("""
        SELECT COUNT(m) > 0
        FROM UmcProductChapterMembership m
        WHERE m.memberActivityPeriod.umcProductMember.id = :umcProductMemberId
          AND m.chapter.id = :chapterId
          AND (:excludedChapterMembershipId IS NULL OR m.id <> :excludedChapterMembershipId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR m.period.startDate <= :endDate)
          AND (m.period.endDate IS NULL OR m.period.endDate >= :startDate)
        """)
    boolean existsOverlappingChapterMembership(
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("chapterId") Long chapterId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedChapterMembershipId") Long excludedChapterMembershipId
    );

    @Modifying
    @Query("""
        DELETE FROM UmcProductChapterMembership m
        WHERE m.memberActivityPeriod.id IN (
            SELECT p.id
            FROM UmcProductMemberActivityPeriod p
            WHERE p.umcProductMember.id = :umcProductMemberId
        )
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
