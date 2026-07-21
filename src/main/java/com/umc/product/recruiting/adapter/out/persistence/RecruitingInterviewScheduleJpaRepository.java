package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

public interface RecruitingInterviewScheduleJpaRepository extends JpaRepository<RecruitingInterviewSchedule, Long> {

    Optional<RecruitingInterviewSchedule> findByApplication_Id(Long applicationId);
}
