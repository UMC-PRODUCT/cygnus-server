package com.umc.product.recruiting.application.port.out;

public interface CheckRecruitingInterviewSessionReferencePort {

    boolean existsConfirmedBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);
}
