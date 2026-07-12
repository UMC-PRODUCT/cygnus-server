package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingRoundInterviewQuestionPersistenceAdapter implements
    LoadRecruitingRoundInterviewQuestionPort,
    SaveRecruitingRoundInterviewQuestionPort {

    private final RecruitingRoundInterviewQuestionJpaRepository repository;

    @Override
    public RecruitingRoundInterviewQuestion getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_ROUND_INTERVIEW_QUESTION_NOT_FOUND
            ));
    }

    @Override
    public List<RecruitingRoundInterviewQuestion> listByRoundId(Long roundId) {
        return repository.findAllByRound_IdOrderByOrderNoAscIdAsc(roundId);
    }

    @Override
    public List<RecruitingRoundInterviewQuestion> listActiveByRoundId(Long roundId) {
        return repository.findAllByRound_IdAndActiveTrueOrderByOrderNoAscIdAsc(roundId);
    }

    @Override
    public RecruitingRoundInterviewQuestion save(RecruitingRoundInterviewQuestion question) {
        return repository.save(question);
    }
}
