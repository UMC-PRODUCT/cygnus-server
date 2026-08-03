package com.umc.product.recruiting.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.CheckRecruitingInterviewSessionReferencePort;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewSessionReferencePersistenceAdapter
    implements CheckRecruitingInterviewSessionReferencePort {

    private final RecruitingInterviewSessionReferenceJpaRepository repository;

    @Override
    public boolean existsConfirmedBySessionId(Long sessionId) {
        return repository.existsByInterviewSessionIdAndStatus(
            sessionId,
            RecruitingInterviewScheduleStatus.CONFIRMED
        );
    }

    @Override
    public boolean existsBySessionId(Long sessionId) {
        return repository.existsByInterviewSessionId(sessionId);
    }
}
