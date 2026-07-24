package com.umc.product.challenger.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyChallengerAuthorizationAdapter
    implements PolicyRolloutEvaluator<ChallengerAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(ChallengerAuthorizationContext context) {
        var snapshot = context.legacySubject().toAuthoritySnapshot();
        boolean allowed = switch (context.action()) {
            case CHALLENGER_CREATE, RECORD_READ ->
                snapshot.isSuperAdmin()
                    || context.legacySubject().roleAttributes().stream()
                        .anyMatch(role -> role.roleType().isAtLeastSchoolCore());
            case CHALLENGER_UPDATE, CHALLENGER_DELETE,
                 POINT_DELETE, RECORD_CREATE, RECORD_DELETE ->
                snapshot.isCentralCoreInAnyGisu();
            case POINT_CREATE, POINT_UPDATE ->
                snapshot.isCentralMemberInGisu(context.targetGisuId())
                    || context.targetSchoolId() != null
                    && snapshot.isSchoolCoreInGisu(
                        context.targetGisuId(),
                        context.targetSchoolId());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
