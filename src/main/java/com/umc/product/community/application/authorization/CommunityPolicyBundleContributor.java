package com.umc.product.community.application.authorization;

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
public class CommunityPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "community-resource";

    @Override
    public String namespace() {
        return CommunityPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return CommunityPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/community/bundle.json"),
            List.of(new PolicyClasspathResource(
                "community-resource.policy.json",
                "policies/community/community-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest(
                "PATCH /api/v1/posts/{postId}",
                "PostController#updatePost",
                CommunityPolicyAction.POST_UPDATE),
            rest(
                "PATCH /api/v1/posts/{postId}/lightning",
                "PostController#updateLightningPost",
                CommunityPolicyAction.POST_UPDATE),
            rest(
                "DELETE /api/v1/posts/{postId}",
                "PostController#deletePost",
                CommunityPolicyAction.POST_DELETE),
            rest(
                "DELETE /api/v1/posts/{postId}/comments/{commentId}",
                "CommentController#deleteComment",
                CommunityPolicyAction.COMMENT_DELETE),
            capability(
                "capability:ResourceType.COMMUNITY_POST",
                "com.umc.product.community.application.service.evaluator."
                    + "CommunityPostPermissionEvaluator#evaluate",
                CommunityPolicyAction.POST_READ),
            capability(
                "capability:ResourceType.COMMUNITY_COMMENT",
                "com.umc.product.community.application.service.evaluator."
                    + "CommunityCommentPermissionEvaluator#evaluate",
                CommunityPolicyAction.COMMENT_READ));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        CommunityPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.community.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.RESOURCE);
    }

    private PolicySurfaceDescriptor capability(
        String id,
        String handler,
        CommunityPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            id,
            handler,
            PolicySurfaceType.INTERNAL_BATCH,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.CAPABILITY);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Community policy schemaVersion이 일치하지 않습니다.");
        require(CommunityPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Community policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Community policy namespace가 일치하지 않습니다.");
        require(CommunityPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Community policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Community policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(CommunityPolicyAction.values())
            .map(CommunityPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Community policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
