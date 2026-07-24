package com.umc.product.audit.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.audit.application.authorization.AuditAuthorizationContext;
import com.umc.product.audit.application.authorization.AuditExpectedDifference;
import com.umc.product.audit.application.authorization.AuditPolicyAction;
import com.umc.product.audit.application.authorization.AuditPolicyDomainSchema;
import com.umc.product.audit.application.authorization.LegacyAuditAuthorizationAdapter;
import com.umc.product.audit.application.authorization.TargetAuditAuthorizationAdapter;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;


@Component
public class AuditLogPermissionEvaluator implements ResourcePermissionEvaluator {

    private final BooleanPolicyRolloutExecutor<AuditAuthorizationContext> rollout;
    private final Clock clock;

    public AuditLogPermissionEvaluator(
        LegacyAuditAuthorizationAdapter legacyEvaluator,
        TargetAuditAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.rollout = new BooleanPolicyRolloutExecutor<>(
            legacyEvaluator,
            targetEvaluator,
            new AuditExpectedDifference(),
            modeResolver,
            observer,
            registry,
            AuditPolicyDomainSchema.BUNDLE_KEY,
            AuditPolicyAction.LIST.id());
        this.clock = clock;
    }

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.AUDIT;
    }


    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> evaluateList(subjectAttributes);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "PE 관련 에러가 발생하였습니다. 관리자에게 문의하세요.");
        };
    }

    private boolean evaluateList(SubjectAttributes subjectAttributes) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(subjectAttributes);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return rollout.evaluate(
            new AuditAuthorizationContext(subjectAttributes, subject, evaluatedAt),
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
