package com.umc.product.inhouse.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.inhouse.domain.UmcProductSquad;

import jakarta.persistence.LockModeType;

public interface UmcProductSquadJpaRepository extends JpaRepository<UmcProductSquad, Long> {

    List<UmcProductSquad> findByIdIn(Collection<Long> ids);

    @Query("""
        SELECT COUNT(s) > 0
        FROM UmcProductSquad s
        WHERE s.code = :code
          AND (:excludedSquadId IS NULL OR s.id <> :excludedSquadId)
        """)
    boolean existsByCode(
        @Param("code") String code,
        @Param("excludedSquadId") Long excludedSquadId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UmcProductSquad s WHERE s.id = :squadId")
    java.util.Optional<UmcProductSquad> findByIdWithLock(@Param("squadId") Long squadId);

    @Query("""
        SELECT s
        FROM UmcProductSquad s
        WHERE (:active IS NULL OR s.isActive = :active)
          AND (CAST(:activeOn AS LocalDate) IS NULL OR (
            s.period.startDate <= :activeOn
            AND (s.period.endDate IS NULL OR s.period.endDate >= :activeOn)
          ))
        ORDER BY s.sortOrder ASC, s.id ASC
        """)
    List<UmcProductSquad> findAll(
        @Param("active") Boolean active,
        @Param("activeOn") LocalDate activeOn
    );
}
