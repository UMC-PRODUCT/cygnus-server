package com.umc.product.authorization.application.port.out;

import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.SubjectAttributes;

public interface SemanticResourcePermissionEvaluator extends ResourcePermissionEvaluator {

    boolean evaluateSemantic(
        SubjectAttributes subject,
        ResourcePermission permission,
        String actionId
    );
}
