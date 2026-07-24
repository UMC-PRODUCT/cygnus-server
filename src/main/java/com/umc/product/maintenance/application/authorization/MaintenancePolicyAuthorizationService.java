package com.umc.product.maintenance.application.authorization;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaintenancePolicyAuthorizationService {

    private final LoadPolicySubjectSnapshotUseCase subjectLoader;
    private final BooleanPolicyRolloutExecutor<MaintenanceAuthorizationContext> rollout;

    public MaintenancePolicyAuthorizationService(
        LoadPolicySubjectSnapshotUseCase subjectLoader,
        LegacyMaintenanceAuthorizationAdapter legacyEvaluator,
        TargetMaintenanceAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry
    ) {
        this.subjectLoader = subjectLoader;
        this.rollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            PolicyExpectedDifference.none(),
            modeResolver,
            observer,
            registry,
            MaintenancePolicyDomainSchema.BUNDLE_KEY,
            MaintenancePolicyAction.BYPASS.id());
    }

    public boolean canBypass(long memberId) {
        try {
            var subject = subjectLoader.loadMemberPolicySubject(memberId);
            return rollout.evaluate(
                new MaintenanceAuthorizationContext(subject),
                subject.evaluatedAt());
        } catch (RuntimeException exception) {
            log.warn(
                "Maintenance policy subject/evaluation에 실패해 bypass를 거부합니다: failureType={}",
                exception.getClass().getSimpleName());
            return false;
        }
    }
}
