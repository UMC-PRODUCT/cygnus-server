package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.umc.product.recruiting.domain.RecruitingInterviewSession;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface RecruitingInterviewSessionJpaRepository extends JpaRepository<RecruitingInterviewSession, Long> {

    List<RecruitingInterviewSession> findAllByRoundIdOrderByStartsAtAscIdAsc(Long roundId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT s FROM RecruitingInterviewSession s WHERE s.id = :id")
    Optional<RecruitingInterviewSession> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT s FROM RecruitingInterviewSession s WHERE s.id IN :ids ORDER BY s.id")
    List<RecruitingInterviewSession> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    void deleteAllByRoundId(Long roundId);
}
