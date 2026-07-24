package com.umc.product.storage.application.authorization;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;

@Service
public class StoragePolicyAuthorizationService {

    private final LoadPolicySubjectSnapshotUseCase subjectLoader;
    private final BooleanPolicyRolloutExecutor<StorageAuthorizationContext> rollout;
    private final Clock clock;

    public StoragePolicyAuthorizationService(
        LoadPolicySubjectSnapshotUseCase subjectLoader,
        LegacyStorageAuthorizationAdapter legacyEvaluator,
        TargetStorageAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.subjectLoader = subjectLoader;
        this.rollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            PolicyExpectedDifference.none(),
            modeResolver,
            observer,
            registry,
            StoragePolicyDomainSchema.BUNDLE_KEY,
            StoragePolicyAction.DELETE_FILE.id());
        this.clock = clock;
    }

    public boolean canDelete(long requesterMemberId, Long uploadedMemberId) {
        boolean uploader = Objects.equals(uploadedMemberId, requesterMemberId);
        Optional<com.umc.product.authorization.domain.AuthorizationSubjectSnapshot> subject =
            uploader ? Optional.empty() : loadSubject(requesterMemberId);
        var evaluatedAt = subject
            .map(com.umc.product.authorization.domain.AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollout.evaluate(
            new StorageAuthorizationContext(uploader, subject, evaluatedAt),
            evaluatedAt);
    }

    private Optional<com.umc.product.authorization.domain.AuthorizationSubjectSnapshot> loadSubject(
        long memberId
    ) {
        try {
            return Optional.of(subjectLoader.loadMemberPolicySubject(memberId));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
