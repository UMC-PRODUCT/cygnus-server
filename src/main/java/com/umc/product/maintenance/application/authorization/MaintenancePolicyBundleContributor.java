package com.umc.product.maintenance.application.authorization;

import java.util.List;

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
public class MaintenancePolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "maintenance-resource";

    @Override
    public String namespace() {
        return MaintenancePolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return MaintenancePolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/maintenance/bundle.json"),
            List.of(new PolicyClasspathResource(
                "maintenance-resource.policy.json",
                "policies/maintenance/maintenance-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        String controller =
            "com.umc.product.maintenance.adapter.in.web.AdminMaintenanceController#";
        return List.of(
            surface(
                "rest:POST /api/v1/maintenance/admin",
                controller + "start",
                PolicySurfaceType.REST,
                PolicySurfaceGate.DIRECT),
            surface(
                "rest:PATCH /api/v1/maintenance/admin/{windowId}/end",
                controller + "forceEnd",
                PolicySurfaceType.REST,
                PolicySurfaceGate.DIRECT),
            surface(
                "rest:GET /api/v1/maintenance/admin",
                controller + "listAll",
                PolicySurfaceType.REST,
                PolicySurfaceGate.DIRECT),
            surface(
                "rest:GET /api/v1/maintenance/admin/{windowId}",
                controller + "getOne",
                PolicySurfaceType.REST,
                PolicySurfaceGate.DIRECT),
            surface(
                "filter:maintenance-bypass",
                "com.umc.product.maintenance.adapter.in.web.filter.MaintenanceFilter#doFilterInternal",
                PolicySurfaceType.INTERNAL_BATCH,
                PolicySurfaceGate.ACTOR));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor surface(
        String id,
        String handler,
        PolicySurfaceType type,
        PolicySurfaceGate gate
    ) {
        return new PolicySurfaceDescriptor(
            MaintenancePolicyDomainSchema.NAMESPACE,
            id,
            handler,
            type,
            MaintenancePolicyAction.BYPASS.id(),
            MODULE_ID,
            gate);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Maintenance policy schemaVersion이 일치하지 않습니다.");
        require(MaintenancePolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Maintenance policy contextSchemaVersion이 일치하지 않습니다.");
        require(MaintenancePolicyDomainSchema.NAMESPACE.equals(bundle.namespace()),
            "Maintenance policy namespace가 일치하지 않습니다.");
        require(MaintenancePolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Maintenance policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Maintenance policy module이 일치하지 않습니다.");
        require(
            bundle.statementsForAction(MaintenancePolicyAction.BYPASS.id()).size() == 1,
            "Maintenance policy action coverage가 일치하지 않습니다.");
        require(bundle.modules().getFirst().statements().stream()
            .allMatch(statement -> statement.outcomes().isEmpty()),
            "Maintenance policy는 outcome을 방출할 수 없습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
