package com.umc.product.schedule.application.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@Service
public class SchedulePolicyAuthorizationService {

    private final Map<SchedulePolicyAction, BooleanPolicyRolloutExecutor<ScheduleAuthorizationContext>>
        rollouts;
    private final LoadSchedulePort loadSchedulePort;
    private final LoadScheduleParticipantPort loadScheduleParticipantPort;
    private final GetGisuUseCase getGisuUseCase;
    private final Clock clock;

    public SchedulePolicyAuthorizationService(
        LegacyScheduleAuthorizationAdapter legacyEvaluator,
        TargetScheduleAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        LoadSchedulePort loadSchedulePort,
        LoadScheduleParticipantPort loadScheduleParticipantPort,
        GetGisuUseCase getGisuUseCase,
        Clock clock
    ) {
        EnumMap<SchedulePolicyAction, BooleanPolicyRolloutExecutor<ScheduleAuthorizationContext>>
            configured = new EnumMap<>(SchedulePolicyAction.class);
        for (SchedulePolicyAction action : SchedulePolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                new ScheduleExpectedDifference(),
                modeResolver,
                observer,
                registry,
                SchedulePolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
        this.loadSchedulePort = loadSchedulePort;
        this.loadScheduleParticipantPort = loadScheduleParticipantPort;
        this.getGisuUseCase = getGisuUseCase;
        this.clock = clock;
    }

    public boolean evaluate(
        SchedulePolicyAction action,
        SubjectAttributes legacySubject,
        Long scheduleId
    ) {
        Optional<AuthorizationSubjectSnapshot> subject = policySubject(legacySubject);
        Instant evaluatedAt = subject
            .map(AuthorizationSubjectSnapshot::evaluatedAt)
            .orElseGet(clock::instant);
        Schedule schedule = scheduleRequired(action, scheduleId)
            ? loadSchedulePort.findById(scheduleId)
                .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND))
            : null;
        boolean author = schedule != null
            && schedule.getAuthorMemberId().equals(legacySubject.memberId());
        boolean participant = action == SchedulePolicyAction.ATTENDANCE_SUBMIT
            && scheduleId != null
            && loadScheduleParticipantPort
                .findByScheduleIdAndMemberId(scheduleId, legacySubject.memberId())
                .isPresent();
        Long targetGisuId = needsTargetGisu(action, scheduleId)
            ? getGisuUseCase.getGisuByDate(schedule.getStartsAt()).gisuId()
            : null;
        ScheduleAuthorizationContext context = new ScheduleAuthorizationContext(
            action,
            legacySubject,
            subject,
            scheduleId != null,
            targetGisuId,
            author,
            participant,
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }

    private boolean scheduleRequired(SchedulePolicyAction action, Long scheduleId) {
        if (scheduleId == null) {
            return false;
        }
        return switch (action) {
            case SCHEDULE_UPDATE, SCHEDULE_DELETE, SCHEDULE_FORCE_DELETE,
                ATTENDANCE_READ, ATTENDANCE_APPROVE -> true;
            default -> false;
        };
    }

    private boolean needsTargetGisu(SchedulePolicyAction action, Long scheduleId) {
        return scheduleId != null
            && (action == SchedulePolicyAction.ATTENDANCE_READ
                || action == SchedulePolicyAction.ATTENDANCE_APPROVE);
    }

    private Optional<AuthorizationSubjectSnapshot> policySubject(SubjectAttributes subject) {
        try {
            return Optional.of(subject.toAuthorizationSubjectSnapshot());
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }
}
