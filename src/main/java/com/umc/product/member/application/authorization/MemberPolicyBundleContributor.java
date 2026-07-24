package com.umc.product.member.application.authorization;

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
public class MemberPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "member-resource";

    @Override
    public String namespace() {
        return MemberPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return MemberPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/member/bundle.json"),
            List.of(new PolicyClasspathResource(
                "member-resource.policy.json",
                "policies/member/member-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:GET /api/v1/members/{memberId}",
                "com.umc.product.member.adapter.in.web.MemberQueryController#getMember",
                PolicySurfaceType.REST,
                MemberPolicyAction.READ.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT),
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:DELETE /api/v1/members/admin/{memberId}",
                "com.umc.product.member.adapter.in.web.MemberCommandController#deleteMemberByAdmin",
                PolicySurfaceType.REST,
                MemberPolicyAction.DELETE.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Member policy schemaVersion이 일치하지 않습니다.");
        require(MemberPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Member policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Member policy namespace가 일치하지 않습니다.");
        require(MemberPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Member policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Member policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(MemberPolicyAction.values())
            .map(MemberPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Member policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
