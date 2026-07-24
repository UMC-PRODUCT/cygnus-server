package com.umc.product.audit.application.authorization;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;
import com.umc.product.authorization.domain.policy.PolicySurfaceGate;
import com.umc.product.authorization.domain.policy.PolicySurfaceType;

@Component
public class AuditPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "audit-resource";

    @Override
    public String namespace() {
        return AuditPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return AuditPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/audit/bundle.json"),
            List.of(new PolicyClasspathResource(
                "audit-resource.policy.json",
                "policies/audit/audit-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(new PolicySurfaceDescriptor(
            AuditPolicyDomainSchema.NAMESPACE,
            "rest:GET /api/v1/audit/admin/audit-logs",
            "com.umc.product.audit.adapter.in.web.AuditLogController#search",
            PolicySurfaceType.REST,
            AuditPolicyAction.LIST.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Audit policy schemaVersion이 일치하지 않습니다.");
        require(AuditPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Audit policy contextSchemaVersion이 일치하지 않습니다.");
        require(AuditPolicyDomainSchema.NAMESPACE.equals(bundle.namespace()),
            "Audit policy namespace가 일치하지 않습니다.");
        require(AuditPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Audit policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Audit policy module이 일치하지 않습니다.");
        Set<String> actions = bundle.modules().getFirst().statements().stream()
            .flatMap(statement -> statement.actions().stream())
            .collect(Collectors.toUnmodifiableSet());
        require(actions.equals(Set.of(AuditPolicyAction.LIST.id())),
            "Audit policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
