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

/**
 * 삭제된(soft delete) 차수는 조회에서 제외한다.
 * 상속 메서드인 {@code findById}는 이 조건을 걸 수 없으므로 사용하지 않고
 * {@link #findActiveById}를 사용한다.
 */
public interface RecruitingRoundJpaRepository extends JpaRepository<RecruitingRound, Long> {

    @Query("SELECT round FROM RecruitingRound round WHERE round.id = :id AND round.deletedAt IS NULL")
    Optional<RecruitingRound> findActiveById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("""
        SELECT round FROM RecruitingRound round
        JOIN FETCH round.season
        WHERE round.id = :id
          AND round.deletedAt IS NULL
        """)
    Optional<RecruitingRound> findByIdForUpdate(@Param("id") Long id);

    /** 복구 전용. 삭제된 차수까지 포함해 잠금 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT round FROM RecruitingRound round JOIN FETCH round.season WHERE round.id = :id")
    Optional<RecruitingRound> findByIdForUpdateIncludingDeleted(@Param("id") Long id);

    List<RecruitingRound> findAllBySeason_IdAndDeletedAtIsNullOrderByRoundNoAscIdAsc(Long seasonId);

    @Query("""
        SELECT round FROM RecruitingRound round
        JOIN FETCH round.season
        WHERE round.season.id IN :seasonIds
          AND round.deletedAt IS NULL
        """)
    List<RecruitingRound> findAllBySeasonIds(@Param("seasonIds") List<Long> seasonIds);

    boolean existsBySeason_IdAndTypeAndRoundNoAndDeletedAtIsNull(
        Long seasonId,
        RecruitingRoundType type,
        Integer roundNo
    );

    boolean existsBySeason_IdAndTitleIgnoreCaseAndDeletedAtIsNull(Long seasonId, String title);

    boolean existsBySeason_IdAndTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(Long seasonId, String title, Long id);

    @Query("""
        SELECT COALESCE(MAX(round.roundNo), 0)
        FROM RecruitingRound round
        WHERE round.season.id = :seasonId
          AND round.type = com.umc.product.recruiting.domain.enums.RecruitingRoundType.ADDITIONAL
          AND round.deletedAt IS NULL
        """)
    int findMaxAdditionalRoundNo(@Param("seasonId") Long seasonId);

}
