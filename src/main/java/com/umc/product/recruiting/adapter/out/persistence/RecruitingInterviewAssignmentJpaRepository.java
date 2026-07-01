package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;

public interface RecruitingInterviewAssignmentJpaRepository extends JpaRepository<RecruitingInterviewAssignment, Long> {

    Optional<RecruitingInterviewAssignment> findByApplication_IdAndInterviewerMemberId(
        Long applicationId,
        Long interviewerMemberId
    );

    List<RecruitingInterviewAssignment> findAllByApplication_IdOrderByStartsAtAscIdAsc(Long applicationId);

    List<RecruitingInterviewAssignment> findAllByInterviewerMemberIdOrderByStartsAtAscIdAsc(Long interviewerMemberId);
}
