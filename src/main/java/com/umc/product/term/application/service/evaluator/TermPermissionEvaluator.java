package com.umc.product.term.application.service.evaluator;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.term.application.authorization.LegacyTermAuthorizationAdapter;
import com.umc.product.term.application.authorization.TargetTermAuthorizationAdapter;
import com.umc.product.term.application.authorization.TermAuthorizationContext;
import com.umc.product.term.application.authorization.TermPolicyAction;
import com.umc.product.term.application.authorization.TermPolicyDomainSchema;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@Component
public class TermPermissionEvaluator implements ResourcePermissionEvaluator {

    private final BooleanPolicyRolloutExecutor<TermAuthorizationContext> rollout;
    private final Clock clock;

    public TermPermissionEvaluator(
        LegacyTermAuthorizationAdapter legacyEvaluator,
        TargetTermAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.rollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            PolicyExpectedDifference.none(),
            modeResolver,
            observer,
            registry,
            TermPolicyDomainSchema.BUNDLE_KEY,
            TermPolicyAction.CREATE.id());
        this.clock = clock;
    }

    /**
     * 이 Evaluator가 처리할 수 있는 ResourceType
     */
    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.TERM;
    }

    /**
     * 특정 리소스에 대한 권한 평가
     */
    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permissionType = resourcePermission.permission();

        return switch (permissionType) {
            case WRITE -> evaluateCreate(subjectAttributes);
            default ->
                throw new TermDomainException(TermErrorCode.TERM_PERMISSION_DENIED, "지원하지 않는 약관 권한 유형이에요. 관리자에게 문의해주세요.");
        };
    }

    private boolean evaluateCreate(SubjectAttributes subjectAttributes) {
        Optional<AuthorizationSubjectSnapshot> policySubject =
            policySubject(subjectAttributes);
        Instant evaluatedAt = policySubject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollout.evaluate(
            new TermAuthorizationContext(subjectAttributes, policySubject, evaluatedAt),
            evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(
        SubjectAttributes subjectAttributes
    ) {
        try {
            return Optional.of(subjectAttributes.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }

}
