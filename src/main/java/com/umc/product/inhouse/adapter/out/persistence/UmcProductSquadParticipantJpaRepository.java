package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductSquadParticipant;
import com.umc.product.inhouse.domain.enums.UmcProductSquadRole;

public interface UmcProductSquadParticipantJpaRepository
    extends JpaRepository<UmcProductSquadParticipant, Long> {

    @Query("""
        SELECT p
        FROM UmcProductSquadParticipant p
        JOIN FETCH p.squad
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE p.squad.id = :squadId
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductSquadParticipant> findAllBySquadId(@Param("squadId") Long squadId);

    @Query("""
        SELECT p
        FROM UmcProductSquadParticipant p
        JOIN FETCH p.squad
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE period.umcProductMember.id = :umcProductMemberId
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductSquadParticipant> findAllByUmcProductMemberId(
        @Param("umcProductMemberId") Long umcProductMemberId
    );

    @Query("""
        SELECT p
        FROM UmcProductSquadParticipant p
        JOIN FETCH p.squad
        JOIN FETCH p.memberActivityPeriod period
        JOIN FETCH period.umcProductMember
        WHERE period.umcProductMember.id IN :umcProductMemberIds
        ORDER BY p.period.startDate DESC, p.id DESC
        """)
    List<UmcProductSquadParticipant> findAllByUmcProductMemberIds(
        @Param("umcProductMemberIds") Collection<Long> umcProductMemberIds
    );

    boolean existsBySquadId(Long squadId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    @Query("""
        SELECT COUNT(p) > 0
        FROM UmcProductSquadParticipant p
        WHERE p.squad.id = :squadId
          AND p.memberActivityPeriod.umcProductMember.id = :umcProductMemberId
          AND (:excludedSquadParticipantId IS NULL OR p.id <> :excludedSquadParticipantId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR p.period.startDate <= :endDate)
          AND (p.period.endDate IS NULL OR p.period.endDate >= :startDate)
        """)
    boolean existsOverlappingMemberInSquad(
        @Param("squadId") Long squadId,
        @Param("umcProductMemberId") Long umcProductMemberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedSquadParticipantId") Long excludedSquadParticipantId
    );

    @Query("""
        SELECT COUNT(p) > 0
        FROM UmcProductSquadParticipant p
        WHERE p.squad.id = :squadId
          AND p.role = :role
          AND (:excludedSquadParticipantId IS NULL OR p.id <> :excludedSquadParticipantId)
          AND (CAST(:endDate AS LocalDate) IS NULL OR p.period.startDate <= :endDate)
          AND (p.period.endDate IS NULL OR p.period.endDate >= :startDate)
        """)
    boolean existsOverlappingSquadLead(
        @Param("squadId") Long squadId,
        @Param("role") UmcProductSquadRole role,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedSquadParticipantId") Long excludedSquadParticipantId
    );

    @Modifying
    @Query("DELETE FROM UmcProductSquadParticipant p WHERE p.squad.id = :squadId")
    void deleteAllBySquadId(@Param("squadId") Long squadId);

    @Modifying
    @Query("""
        DELETE FROM UmcProductSquadParticipant p
        WHERE p.memberActivityPeriod.id IN (
            SELECT period.id
            FROM UmcProductMemberActivityPeriod period
            WHERE period.umcProductMember.id = :umcProductMemberId
        )
        """)
    void deleteAllByUmcProductMemberId(@Param("umcProductMemberId") Long umcProductMemberId);
}
