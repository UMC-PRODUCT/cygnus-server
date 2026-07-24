package com.umc.product.term.application.authorization;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;

@Component
public class TermPolicyBundleContributor implements PolicyBundleContributor {

    @Override
    public String namespace() {
        return TermPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return TermPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/term/bundle.json"),
            List.of(new PolicyClasspathResource(
                "term-resource.policy.json",
                "policies/term/term-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return new TermPolicyCompiledContractValidator();
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return TermPolicySurfaceCatalog.surfaces();
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }
}
