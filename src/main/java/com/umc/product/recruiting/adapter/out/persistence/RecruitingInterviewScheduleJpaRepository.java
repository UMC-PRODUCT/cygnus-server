package com.umc.product.recruiting.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface RecruitingInterviewScheduleJpaRepository extends JpaRepository<RecruitingInterviewSchedule, Long> {

    Optional<RecruitingInterviewSchedule> findByApplication_Id(Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT s FROM RecruitingInterviewSchedule s WHERE s.application.id IN :applicationIds "
        + "ORDER BY s.application.id")
    List<RecruitingInterviewSchedule> findAllByApplicationIdsForUpdate(
        @Param("applicationIds") List<Long> applicationIds
    );

    boolean existsByInterviewSessionIdAndStartsAtAndStatus(
        Long interviewSessionId,
        Instant startsAt,
        com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus status
    );
}
