package com.umc.product.notice.application.authorization;

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
import com.umc.product.notice.application.port.in.query.GetNoticeTargetUseCase;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTargetPattern;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

@Service
public class NoticePolicyAuthorizationService {

    private final Map<NoticePolicyAction, BooleanPolicyRolloutExecutor<NoticeAuthorizationContext>>
        rollouts;
    private final GetNoticeTargetUseCase getNoticeTargetUseCase;
    private final LoadNoticePort loadNoticePort;
    private final ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider;
    private final Clock clock;

    public NoticePolicyAuthorizationService(
        LegacyNoticeAuthorizationAdapter legacyEvaluator,
        TargetNoticeAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        GetNoticeTargetUseCase getNoticeTargetUseCase,
        LoadNoticePort loadNoticePort,
        ObjectProvider<LoadPolicySubjectSnapshotUseCase> subjectLoaderProvider,
        Clock clock
    ) {
        EnumMap<NoticePolicyAction, BooleanPolicyRolloutExecutor<NoticeAuthorizationContext>>
            configured = new EnumMap<>(NoticePolicyAction.class);
        for (NoticePolicyAction action : NoticePolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new NoticeExpectedDifference(),
                modeResolver,
                observer,
                registry,
                NoticePolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.getNoticeTargetUseCase = getNoticeTargetUseCase;
        this.loadNoticePort = loadNoticePort;
        this.subjectLoaderProvider = subjectLoaderProvider;
        this.clock = clock;
    }

    public boolean evaluateResource(
        NoticePolicyAction action,
        SubjectAttributes legacySubject,
        long noticeId
    ) {
        NoticeTargetInfo target = getNoticeTargetUseCase.findByNoticeId(noticeId);
        boolean author = false;
        if (action == NoticePolicyAction.UPDATE || action == NoticePolicyAction.DELETE) {
            Notice notice = loadNoticePort.findNoticeById(noticeId)
                .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));
            author = notice.getAuthorMemberId().equals(legacySubject.memberId());
        }
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        NoticeAuthorizationContext context = new NoticeAuthorizationContext(
            action,
            legacySubject.memberId(),
            Optional.of(legacySubject),
            subject,
            target,
            null,
            author,
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }

    public boolean evaluateCreate(long memberId, NoticeTargetInfo target) {
        NoticeTargetPattern pattern = NoticeTargetPattern.from(target);
        AuthorizationSubjectSnapshot subject =
            subjectLoaderProvider.getObject().loadMemberPolicySubject(memberId);
        Instant evaluatedAt = subject.evaluatedAt();
        NoticeAuthorizationContext context = new NoticeAuthorizationContext(
            NoticePolicyAction.CREATE,
            memberId,
            Optional.empty(),
            Optional.of(subject),
            target,
            pattern,
            false,
            evaluatedAt);
        return rollouts.get(NoticePolicyAction.CREATE).evaluate(context, evaluatedAt);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
