package com.umc.product.authorization.application.authorization;

import java.util.Arrays;
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
public class AuthorizationPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "challenger-role-resource";

    @Override
    public String namespace() {
        return AuthorizationPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return AuthorizationPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/authorization/bundle.json"),
            List.of(new PolicyClasspathResource(
                "challenger-role-resource.policy.json",
                "policies/authorization/challenger-role-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            surface("rest:GET /api/v1/authorization/challenger-role/{challengerRoleId}",
                "getChallengerRole", AuthorizationPolicyAction.CHALLENGER_ROLE_READ),
            surface("rest:POST /api/v1/authorization/challenger-role",
                "createChallengerRole", AuthorizationPolicyAction.CHALLENGER_ROLE_CREATE),
            surface("rest:DELETE /api/v1/authorization/challenger-role/{challengerRoleId}",
                "deleteChallengerRole", AuthorizationPolicyAction.CHALLENGER_ROLE_DELETE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor surface(
        String surface,
        String method,
        AuthorizationPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            surface,
            "com.umc.product.authorization.adapter.in.web.ChallengerRoleController#" + method,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Authorization policy schemaVersion이 일치하지 않습니다.");
        require(AuthorizationPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Authorization policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Authorization policy namespace가 일치하지 않습니다.");
        require(AuthorizationPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Authorization policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Authorization policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(AuthorizationPolicyAction.values())
            .map(AuthorizationPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Authorization policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
