package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSessionPort;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewSessionPersistenceAdapter
    implements LoadRecruitingInterviewSessionPort, SaveRecruitingInterviewSessionPort {

    private final RecruitingInterviewSessionJpaRepository repository;

    @Override
    public RecruitingInterviewSession getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_NOT_FOUND
            ));
    }

    @Override
    public List<RecruitingInterviewSession> getAllByIdsForUpdate(List<Long> ids) {
        return RecruitingLockExceptionTranslator.translateAssignment(
            () -> repository.findAllByIdInForUpdate(ids)
        );
    }

    @Override
    public List<RecruitingInterviewSession> listByRoundId(Long roundId) {
        return repository.findAllByRoundIdOrderByStartsAtAscIdAsc(roundId);
    }

    @Override
    public RecruitingInterviewSession save(RecruitingInterviewSession session) {
        return repository.save(session);
    }

    @Override
    public void delete(RecruitingInterviewSession session) {
        repository.delete(session);
    }
}
