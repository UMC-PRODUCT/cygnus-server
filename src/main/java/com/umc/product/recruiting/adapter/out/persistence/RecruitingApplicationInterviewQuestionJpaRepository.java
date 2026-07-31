package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;

public interface RecruitingApplicationInterviewQuestionJpaRepository
    extends JpaRepository<RecruitingApplicationInterviewQuestion, Long> {

    List<RecruitingApplicationInterviewQuestion> findAllByApplication_IdOrderByOrderNoAscIdAsc(Long applicationId);

    List<RecruitingApplicationInterviewQuestion> findAllByApplication_IdAndActiveTrueOrderByOrderNoAscIdAsc(
        Long applicationId
    );
}
