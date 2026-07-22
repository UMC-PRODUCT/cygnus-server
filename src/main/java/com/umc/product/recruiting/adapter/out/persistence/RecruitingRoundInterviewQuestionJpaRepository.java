package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;

public interface RecruitingRoundInterviewQuestionJpaRepository
    extends JpaRepository<RecruitingRoundInterviewQuestion, Long> {

    List<RecruitingRoundInterviewQuestion> findAllByRound_IdOrderByOrderNoAscIdAsc(Long roundId);

    List<RecruitingRoundInterviewQuestion> findAllByRound_IdAndActiveTrueOrderByOrderNoAscIdAsc(Long roundId);

    List<RecruitingRoundInterviewQuestion> findAllByRound_IdInAndActiveTrueOrderByRound_IdAscOrderNoAscIdAsc(
        Set<Long> roundIds
    );

    void deleteAllByRound_Id(Long roundId);
}
