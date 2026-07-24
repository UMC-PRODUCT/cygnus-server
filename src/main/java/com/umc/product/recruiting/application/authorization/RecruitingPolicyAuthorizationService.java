package com.umc.product.recruiting.application.authorization;

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
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingSeason;

@Service
public class RecruitingPolicyAuthorizationService {

    private final Map<RecruitingPolicyAction, BooleanPolicyRolloutExecutor<RecruitingAuthorizationContext>>
        rollouts;
    private final LoadRecruitingSeasonPort loadRecruitingSeasonPort;
    private final ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider;
    private final Clock clock;

    public RecruitingPolicyAuthorizationService(
        LegacyRecruitingAuthorizationAdapter legacyEvaluator,
        TargetRecruitingAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        LoadRecruitingSeasonPort loadRecruitingSeasonPort,
        ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider,
        Clock clock
    ) {
        EnumMap<RecruitingPolicyAction, BooleanPolicyRolloutExecutor<RecruitingAuthorizationContext>>
            configured = new EnumMap<>(RecruitingPolicyAction.class);
        for (RecruitingPolicyAction action : RecruitingPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new RecruitingExpectedDifference(),
                modeResolver,
                observer,
                registry,
                RecruitingPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.loadRecruitingSeasonPort = loadRecruitingSeasonPort;
        this.subjectLoaderProvider = subjectLoaderProvider;
        this.clock = clock;
    }

    public boolean evaluateResource(
        RecruitingPolicyAction action,
        SubjectAttributes legacySubject,
        Long seasonId
    ) {
        RecruitingSeason season = seasonId == null
            ? null
            : loadRecruitingSeasonPort.getById(seasonId);
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        RecruitingAuthorizationContext context = new RecruitingAuthorizationContext(
            action,
            legacySubject.memberId(),
            Optional.of(legacySubject),
            subject,
            season != null,
            season == null ? null : season.getGisuId(),
            season == null ? null : season.getSchoolId(),
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }

    public boolean evaluateMember(
        RecruitingPolicyAction action,
        long memberId,
        Long targetGisuId,
        Long targetSchoolId
    ) {
        AuthorizationSubjectSnapshot subject =
            subjectLoaderProvider.getObject().loadMemberPolicySubject(memberId);
        Instant evaluatedAt = subject.evaluatedAt();
        RecruitingAuthorizationContext context = new RecruitingAuthorizationContext(
            action,
            memberId,
            Optional.empty(),
            Optional.of(subject),
            targetGisuId != null,
            targetGisuId,
            targetSchoolId,
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
