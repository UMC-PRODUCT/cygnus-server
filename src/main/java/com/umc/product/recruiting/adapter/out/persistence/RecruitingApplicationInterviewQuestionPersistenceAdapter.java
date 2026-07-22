package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationInterviewQuestionPersistenceAdapter implements
    LoadRecruitingApplicationInterviewQuestionPort,
    SaveRecruitingApplicationInterviewQuestionPort {

    private final RecruitingApplicationInterviewQuestionJpaRepository repository;

    @Override
    public RecruitingApplicationInterviewQuestion getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_APPLICATION_INTERVIEW_QUESTION_NOT_FOUND
            ));
    }

    @Override
    public List<RecruitingApplicationInterviewQuestion> listByApplicationId(Long applicationId) {
        return repository.findAllByApplication_IdOrderByOrderNoAscIdAsc(applicationId);
    }

    @Override
    public List<RecruitingApplicationInterviewQuestion> listActiveByApplicationId(Long applicationId) {
        return repository.findAllByApplication_IdAndActiveTrueOrderByOrderNoAscIdAsc(applicationId);
    }

    @Override
    public Map<Long, List<RecruitingApplicationInterviewQuestion>> listActiveByApplicationIds(
        Set<Long> applicationIds
    ) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }
        return repository
            .findAllByApplication_IdInAndActiveTrueOrderByApplication_IdAscOrderNoAscIdAsc(applicationIds)
            .stream()
            .collect(Collectors.groupingBy(question -> question.getApplication().getId()));
    }

    @Override
    public RecruitingApplicationInterviewQuestion save(RecruitingApplicationInterviewQuestion question) {
        return repository.save(question);
    }
}
