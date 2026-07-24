package com.umc.product.organization.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyOrganizationAuthorizationAdapter
    implements PolicyRolloutEvaluator<OrganizationAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(OrganizationAuthorizationContext context) {
        var snapshot = context.legacySubject().toAuthoritySnapshot();
        boolean allowed = switch (context.action()) {
            case GISU_CREATE, GISU_UPDATE, GISU_DELETE,
                 CHAPTER_CREATE, CHAPTER_DELETE,
                 SCHOOL_CREATE, SCHOOL_UPDATE, SCHOOL_DELETE ->
                snapshot.isCentralCoreInAnyGisu();
            case STUDY_GROUP_READ ->
                snapshot.isSchoolAdminInAnyGisu(context.legacySubject().schoolId());
            case STUDY_GROUP_CREATE, STUDY_GROUP_UPDATE, STUDY_GROUP_DELETE ->
                snapshot.isSchoolCoreInAnyGisu(context.legacySubject().schoolId());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
