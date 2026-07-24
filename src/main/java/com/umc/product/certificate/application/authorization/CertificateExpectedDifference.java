package com.umc.product.certificate.application.authorization;

import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;

public final class CertificateExpectedDifference
    implements PolicyExpectedDifference<CertificateAuthorizationContext, Boolean> {

    @Override
    public boolean matches(
        CertificateAuthorizationContext context,
        Boolean legacyDecision,
        Boolean targetDecision
    ) {
        return legacyDecision
            && !targetDecision
            && !context.subject().isSuperAdmin()
            && context.legacyCentralCoreInTargetGisu()
            && !context.activeCentralCoreInTargetGisu();
    }
}
