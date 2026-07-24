package com.umc.product.schedule.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class LegacyScheduleAuthorizationAdapter
    implements PolicyRolloutEvaluator<ScheduleAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(ScheduleAuthorizationContext context) {
        boolean superAdmin = context.legacySubject().toAuthoritySnapshot().isSuperAdmin();
        boolean allowed = switch (context.action()) {
            case SCHEDULE_READ, SCHEDULE_CREATE ->
                superAdmin || !context.legacySubject().gisuChallengerInfos().isEmpty();
            case SCHEDULE_UPDATE, SCHEDULE_DELETE ->
                context.resourceSpecified() && (superAdmin || context.author());
            case SCHEDULE_FORCE_DELETE -> context.resourceSpecified() && superAdmin;
            case ATTENDANCE_SUBMIT ->
                context.resourceSpecified()
                    && !context.legacySubject().gisuChallengerInfos().isEmpty()
                    && context.participant();
            case ATTENDANCE_READ -> context.targetGisuId() == null
                ? superAdmin || hasOperatingRole(context, null)
                : superAdmin || hasOperatingRole(context, context.targetGisuId());
            case ATTENDANCE_APPROVE ->
                context.resourceSpecified()
                    && (superAdmin || hasOperatingRole(context, context.targetGisuId()));
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }

    private boolean hasOperatingRole(ScheduleAuthorizationContext context, Long gisuId) {
        return context.legacySubject().roleAttributes().stream()
            .filter(role -> gisuId == null || role.gisuId().equals(gisuId))
            .map(role -> role.roleType())
            .anyMatch(this::isOperatingRole);
    }

    private boolean isOperatingRole(ChallengerRoleType roleType) {
        return roleType.isAtLeastCentralMember() || roleType.isAtLeastSchoolAdmin();
    }
}
