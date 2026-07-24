package com.umc.product.organization.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class OrganizationExpectedDifference
    implements PolicyExpectedDifference<OrganizationAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        OrganizationAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        if (!legacyDecision || targetDecision || context.subject().isEmpty()) {
            return false;
        }
        var subject = context.subject().orElseThrow();
        if (subject.isSuperAdmin()) {
            return false;
        }
        return context.legacySubject().roleAttributes().stream().anyMatch(role ->
            subject.roles().stream()
                .filter(target -> target.gisuId() == role.gisuId())
                .filter(target -> target.roleType() == role.roleType())
                .filter(target -> java.util.Objects.equals(
                    target.organizationId(),
                    role.organizationId()))
                .noneMatch(target -> target.isActiveAt(context.evaluatedAt())));
    }
}
