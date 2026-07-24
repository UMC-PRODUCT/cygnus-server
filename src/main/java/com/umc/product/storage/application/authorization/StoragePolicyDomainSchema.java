package com.umc.product.storage.application.authorization;

import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class StoragePolicyDomainSchema {

    public static final String NAMESPACE = "storage";
    public static final String VERSION = "storage-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private StoragePolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        return new PolicyDomainSchema(
            VERSION,
            List.of(new ActionSchema(
                StoragePolicyAction.DELETE_FILE.id(),
                Set.of(StoragePolicyAttributes.UPLOADER.name()),
                Set.of(StoragePolicyAttributes.SUPER_ADMIN.name()),
                Set.of())),
            List.of(
                new AttributeSchema(
                    StoragePolicyAttributes.UPLOADER.name(),
                    StoragePolicyAttributes.UPLOADER.type(),
                    Set.of()),
                new AttributeSchema(
                    StoragePolicyAttributes.SUPER_ADMIN.name(),
                    StoragePolicyAttributes.SUPER_ADMIN.type(),
                    Set.of())),
            List.of());
    }
}
