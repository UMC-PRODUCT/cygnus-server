package com.umc.product.authorization.application.port.out.policy;

import java.util.List;

import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;

public interface PolicyBundleContributor {

    String namespace();

    PolicyDomainSchema domainSchema();

    PolicyResourceManifest resourceManifest();

    default CompiledPolicyContractValidator compiledContractValidator() {
        return CompiledPolicyContractValidator.NO_OP;
    }

    default List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of();
    }

    default boolean commonRolloutEnabled() {
        return false;
    }
}
