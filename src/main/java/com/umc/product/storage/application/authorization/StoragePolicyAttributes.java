package com.umc.product.storage.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class StoragePolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> UPLOADER =
        new PolicyAttributeKey<>("relation.isUploader", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN =
        new PolicyAttributeKey<>("relation.superAdmin", PolicyValueType.BOOLEAN);

    private StoragePolicyAttributes() {
    }
}
