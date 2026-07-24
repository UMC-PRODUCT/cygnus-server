package com.umc.product.certificate.application.authorization;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;

@Service
public class CertificatePolicyAuthorizationService {

    private final LoadPolicySubjectSnapshotUseCase subjectLoader;
    private final BooleanPolicyRolloutExecutor<CertificateAuthorizationContext> issueRollout;
    private final BooleanPolicyRolloutExecutor<CertificateAuthorizationContext> revokeRollout;

    public CertificatePolicyAuthorizationService(
        LoadPolicySubjectSnapshotUseCase subjectLoader,
        LegacyCertificateAuthorizationAdapter legacyEvaluator,
        TargetCertificateAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry
    ) {
        this.subjectLoader = subjectLoader;
        var expectedDifference = new CertificateExpectedDifference();
        this.issueRollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            expectedDifference,
            modeResolver,
            observer,
            registry,
            CertificatePolicyDomainSchema.BUNDLE_KEY,
            CertificatePolicyAction.ISSUE_ADMIN.id());
        this.revokeRollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            expectedDifference,
            modeResolver,
            observer,
            registry,
            CertificatePolicyDomainSchema.BUNDLE_KEY,
            CertificatePolicyAction.REVOKE.id());
    }

    public boolean canManage(
        long memberId,
        long targetGisuId,
        CertificatePolicyAction action
    ) {
        try {
            var subject = subjectLoader.loadMemberPolicySubject(memberId);
            var context = new CertificateAuthorizationContext(action, targetGisuId, subject);
            return rollout(action).evaluate(context, subject.evaluatedAt());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private BooleanPolicyRolloutExecutor<CertificateAuthorizationContext> rollout(
        CertificatePolicyAction action
    ) {
        return action == CertificatePolicyAction.ISSUE_ADMIN ? issueRollout : revokeRollout;
    }
}
