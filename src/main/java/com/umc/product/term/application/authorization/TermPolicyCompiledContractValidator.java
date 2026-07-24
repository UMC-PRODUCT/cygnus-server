package com.umc.product.term.application.authorization;

import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

public final class TermPolicyCompiledContractValidator
    implements CompiledPolicyContractValidator {

    private static final String SCHEMA_VERSION = "1.0";
    private static final String MODULE_ID = "term-resource";
    private static final String MODULE_FILENAME = "term-resource.policy.json";

    @Override
    public void validate(CompiledPolicyBundle bundle) {
        require(SCHEMA_VERSION.equals(bundle.schemaVersion()), "Term policy schemaVersion이 일치하지 않습니다.");
        require(TermPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Term policy contextSchemaVersion이 일치하지 않습니다.");
        require(TermPolicyDomainSchema.NAMESPACE.equals(bundle.namespace()),
            "Term policy namespace가 일치하지 않습니다.");
        require(TermPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Term policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1, "Term policy module은 정확히 하나여야 합니다.");
        var module = bundle.modules().getFirst();
        require(MODULE_ID.equals(module.id()), "Term policy module ID가 일치하지 않습니다.");
        require(MODULE_FILENAME.equals(module.filename()), "Term policy module filename이 일치하지 않습니다.");

        Set<String> coveredActions = module.statements().stream()
            .flatMap(statement -> statement.actions().stream())
            .collect(Collectors.toUnmodifiableSet());
        Set<String> expectedActions = java.util.Arrays.stream(TermPolicyAction.values())
            .map(TermPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(coveredActions.equals(expectedActions), "Term policy action coverage가 일치하지 않습니다.");
        require(module.statements().stream().allMatch(statement -> statement.outcomes().isEmpty()),
            "Term policy는 outcome을 방출할 수 없습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
