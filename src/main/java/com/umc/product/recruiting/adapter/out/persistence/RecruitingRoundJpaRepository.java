package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface RecruitingRoundJpaRepository extends JpaRepository<RecruitingRound, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT round FROM RecruitingRound round JOIN FETCH round.season WHERE round.id = :id")
    Optional<RecruitingRound> findByIdForUpdate(@Param("id") Long id);

    List<RecruitingRound> findAllBySeason_IdOrderByRoundNoAscIdAsc(Long seasonId);

    @Query("SELECT round FROM RecruitingRound round JOIN FETCH round.season WHERE round.season.id IN :seasonIds")
    List<RecruitingRound> findAllBySeasonIds(@Param("seasonIds") List<Long> seasonIds);

    boolean existsBySeason_IdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo);

    boolean existsBySeason_IdAndTitleIgnoreCase(Long seasonId, String title);

    boolean existsBySeason_IdAndTitleIgnoreCaseAndIdNot(Long seasonId, String title, Long id);

    @Query("""
        SELECT COALESCE(MAX(round.roundNo), 0)
        FROM RecruitingRound round
        WHERE round.season.id = :seasonId
          AND round.type = com.umc.product.recruiting.domain.enums.RecruitingRoundType.ADDITIONAL
        """)
    int findMaxAdditionalRoundNo(@Param("seasonId") Long seasonId);

}
