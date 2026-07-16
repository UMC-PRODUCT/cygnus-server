package com.umc.product.maintenance.adapter.out.bypass;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;

import lombok.RequiredArgsConstructor;

/**
 * member system role 기반 bypass 구현.
 * <p>
 * 통과 기준: 해당 사용자가 {@code SUPER_ADMIN} system role을 보유하면 통과.
 */
@Component
@RequiredArgsConstructor
public class ChallengerRoleBasedBypassPolicy implements MaintenanceBypassPolicy {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public boolean shouldBypass(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return getChallengerRoleUseCase.isSuperAdmin(memberId);
    }
}
