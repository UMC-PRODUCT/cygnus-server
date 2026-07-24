package com.umc.product.blog.application.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.port.in.policy.LoadPolicySubjectSnapshotUseCase;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;

@Service
public class BlogPolicyAuthorizationService {

    private final ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider;
    private final Map<BlogPolicyAction, BooleanPolicyRolloutExecutor<BlogAuthorizationContext>> rollouts;
    private final Clock clock;

    public BlogPolicyAuthorizationService(
        ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider,
        LegacyBlogAuthorizationAdapter legacyEvaluator,
        TargetBlogAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.subjectLoaderProvider = subjectLoaderProvider;
        this.clock = clock;
        EnumMap<BlogPolicyAction, BooleanPolicyRolloutExecutor<BlogAuthorizationContext>> configured =
            new EnumMap<>(BlogPolicyAction.class);
        for (BlogPolicyAction action : BlogPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                PolicyExpectedDifference.none(),
                modeResolver,
                observer,
                registry,
                BlogPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
    }

    public boolean evaluate(
        BlogPolicyAction action,
        SubjectAttributes legacySubject,
        boolean author
    ) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        return evaluate(new BlogAuthorizationContext(
            action,
            author,
            legacySubject.toAuthoritySnapshot().isSuperAdmin(),
            subject,
            evaluatedAt));
    }

    public boolean isSuperAdminViewer(Long memberId) {
        if (memberId == null) {
            return false;
        }
        try {
            AuthorizationSubjectSnapshot subject =
                subjectLoaderProvider.getObject().loadMemberPolicySubject(memberId);
            return evaluate(new BlogAuthorizationContext(
                BlogPolicyAction.ADMIN_VIEW,
                false,
                subject.isSuperAdmin(),
                Optional.of(subject),
                subject.evaluatedAt()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean evaluate(BlogAuthorizationContext context) {
        return rollouts.get(context.action()).evaluate(context, context.evaluatedAt());
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
