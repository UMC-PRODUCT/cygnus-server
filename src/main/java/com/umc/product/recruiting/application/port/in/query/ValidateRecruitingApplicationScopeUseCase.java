package com.umc.product.recruiting.application.port.in.query;

public interface ValidateRecruitingApplicationScopeUseCase {

    void validateRoundScope(Long applicationId, Long roundId);
}
