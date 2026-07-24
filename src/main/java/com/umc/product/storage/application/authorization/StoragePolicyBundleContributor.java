package com.umc.product.storage.application.authorization;

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
public class StoragePolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "storage-resource";

    @Override
    public String namespace() {
        return StoragePolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return StoragePolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/storage/bundle.json"),
            List.of(new PolicyClasspathResource(
                "storage-resource.policy.json",
                "policies/storage/storage-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(new PolicySurfaceDescriptor(
            namespace(),
            "rest:DELETE /api/v1/storage/{fileId}",
            "com.umc.product.storage.adapter.in.web.StorageController#deleteFile",
            PolicySurfaceType.REST,
            StoragePolicyAction.DELETE_FILE.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Storage policy schemaVersion이 일치하지 않습니다.");
        require(StoragePolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Storage policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Storage policy namespace가 일치하지 않습니다.");
        require(StoragePolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Storage policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Storage policy module이 일치하지 않습니다.");
        require(bundle.statementsForAction(StoragePolicyAction.DELETE_FILE.id()).size() == 2,
            "Storage policy action coverage가 일치하지 않습니다.");
        require(bundle.modules().getFirst().statements().stream()
            .allMatch(statement -> statement.outcomes().isEmpty()),
            "Storage policy는 outcome을 방출할 수 없습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
