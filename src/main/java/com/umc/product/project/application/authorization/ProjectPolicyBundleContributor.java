package com.umc.product.project.application.authorization;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;
import com.umc.product.authorization.domain.policy.PolicySurfaceGate;
import com.umc.product.authorization.domain.policy.PolicySurfaceType;

@Component
public class ProjectPolicyBundleContributor implements PolicyBundleContributor {

    @Override
    public String namespace() {
        return "project";
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return ProjectPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            resource(ProjectPolicyResourceManifest.BUNDLE),
            ProjectPolicyResourceManifest.MODULES.stream()
                .map(this::resource)
                .toList());
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return new ProjectPolicyCompiledContractValidator()::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return ProjectPolicySurfaceCatalog.surfaces().stream()
            .map(surface -> new PolicySurfaceDescriptor(
                "project",
                surface.id(),
                surface.handler(),
                PolicySurfaceType.valueOf(surface.type().name()),
                surface.action().id(),
                surface.module().id(),
                PolicySurfaceGate.valueOf(surface.gate().name())))
            .toList();
    }

    private PolicyClasspathResource resource(ProjectPolicyResourceManifest.PolicyResource resource) {
        return new PolicyClasspathResource(resource.logicalFilename(), resource.classpathPath());
    }
}
