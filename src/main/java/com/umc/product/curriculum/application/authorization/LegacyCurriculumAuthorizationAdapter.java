package com.umc.product.curriculum.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyCurriculumAuthorizationAdapter
    implements PolicyRolloutEvaluator<CurriculumAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CurriculumAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case ORIGINAL_WORKBOOK_MANAGE, ORIGINAL_WORKBOOK_RELEASE ->
                context.legacySubject().toAuthoritySnapshot().isCentralMemberInAnyGisu();
            case WORKBOOK_SUBMISSION_READ ->
                context.legacySubject().toAuthoritySnapshot().isSuperAdmin()
                    || context.legacySubject().roleAttributes().stream()
                        .anyMatch(role -> role.roleType().isAtLeastSchoolAdmin());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
