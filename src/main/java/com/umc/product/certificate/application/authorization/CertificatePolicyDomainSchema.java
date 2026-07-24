package com.umc.product.certificate.application.authorization;

import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class CertificatePolicyDomainSchema {

    public static final String NAMESPACE = "certificate";
    public static final String VERSION = "certificate-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private CertificatePolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> requiredAttributes = Set.of(
            CertificatePolicyAttributes.SUPER_ADMIN.name(),
            CertificatePolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET_GISU.name());
        return new PolicyDomainSchema(
            VERSION,
            List.of(
                new ActionSchema(
                    CertificatePolicyAction.ISSUE_ADMIN.id(),
                    requiredAttributes,
                    Set.of(),
                    Set.of()),
                new ActionSchema(
                    CertificatePolicyAction.REVOKE.id(),
                    requiredAttributes,
                    Set.of(),
                    Set.of())),
            List.of(
                new AttributeSchema(
                    CertificatePolicyAttributes.SUPER_ADMIN.name(),
                    CertificatePolicyAttributes.SUPER_ADMIN.type(),
                    Set.of()),
                new AttributeSchema(
                    CertificatePolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET_GISU.name(),
                    CertificatePolicyAttributes.ACTIVE_CENTRAL_CORE_IN_TARGET_GISU.type(),
                    Set.of())),
            List.of());
    }
}
