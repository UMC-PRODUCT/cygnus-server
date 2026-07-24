package com.umc.product.term.application.authorization;

import java.util.List;

import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;
import com.umc.product.authorization.domain.policy.PolicySurfaceGate;
import com.umc.product.authorization.domain.policy.PolicySurfaceType;

public final class TermPolicySurfaceCatalog {

    private static final List<PolicySurfaceDescriptor> SURFACES = List.of(
        new PolicySurfaceDescriptor(
            TermPolicyDomainSchema.NAMESPACE,
            "rest:POST /api/v1/terms",
            "com.umc.product.term.adapter.in.web.TermController#createTerms",
            PolicySurfaceType.REST,
            TermPolicyAction.CREATE.id(),
            "term-resource",
            PolicySurfaceGate.DIRECT)
    );

    private TermPolicySurfaceCatalog() {
    }

    public static List<PolicySurfaceDescriptor> surfaces() {
        return SURFACES;
    }
}
