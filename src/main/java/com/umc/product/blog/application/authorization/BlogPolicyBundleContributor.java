package com.umc.product.blog.application.authorization;

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
public class BlogPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "blog-resource";

    @Override
    public String namespace() {
        return BlogPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return BlogPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/blog/bundle.json"),
            List.of(new PolicyClasspathResource(
                "blog-resource.policy.json",
                "policies/blog/blog-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        String content = "com.umc.product.blog.adapter.in.web.BlogContentController#";
        String series = "com.umc.product.blog.adapter.in.web.BlogSeriesController#";
        String interaction = "com.umc.product.blog.adapter.in.web.BlogInteractionController#";
        return List.of(
            surface("rest:GET /api/v1/blog/contents/{contentId}/preview",
                content + "getPreview", BlogPolicyAction.CONTENT_READ, PolicySurfaceGate.RESOURCE),
            surface("rest:POST /api/v1/blog/contents",
                content + "create", BlogPolicyAction.CONTENT_CREATE, PolicySurfaceGate.ACTOR),
            surface("rest:PATCH /api/v1/blog/contents/{contentId}",
                content + "update", BlogPolicyAction.CONTENT_UPDATE, PolicySurfaceGate.RESOURCE),
            surface("rest:DELETE /api/v1/blog/contents/{contentId}",
                content + "delete", BlogPolicyAction.CONTENT_DELETE, PolicySurfaceGate.RESOURCE),
            surface("rest:GET /api/v1/blog/series/{seriesId}/preview",
                series + "getPreview", BlogPolicyAction.SERIES_READ, PolicySurfaceGate.RESOURCE),
            surface("rest:POST /api/v1/blog/series",
                series + "create", BlogPolicyAction.SERIES_CREATE, PolicySurfaceGate.ACTOR),
            surface("rest:PATCH /api/v1/blog/series/{seriesId}",
                series + "update", BlogPolicyAction.SERIES_UPDATE, PolicySurfaceGate.RESOURCE),
            surface("rest:DELETE /api/v1/blog/series/{seriesId}",
                series + "delete", BlogPolicyAction.SERIES_DELETE, PolicySurfaceGate.RESOURCE),
            surface("rest:PUT /api/v1/blog/series/{seriesId}/contents",
                series + "replaceContents", BlogPolicyAction.SERIES_UPDATE, PolicySurfaceGate.RESOURCE),
            surface("rest:PATCH /api/v1/blog/{type}/{slug}/comments/{commentId}",
                interaction + "updateComment", BlogPolicyAction.COMMENT_UPDATE, PolicySurfaceGate.RESOURCE),
            surface("rest:DELETE /api/v1/blog/{type}/{slug}/comments/{commentId}",
                interaction + "deleteComment", BlogPolicyAction.COMMENT_DELETE, PolicySurfaceGate.RESOURCE),
            surface("internal:blog-admin-view",
                "com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService#isSuperAdminViewer",
                BlogPolicyAction.ADMIN_VIEW, PolicySurfaceGate.TRANSITIVE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor surface(
        String id,
        String handler,
        BlogPolicyAction action,
        PolicySurfaceGate gate
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            id,
            handler,
            id.startsWith("rest:") ? PolicySurfaceType.REST : PolicySurfaceType.INTERNAL_BATCH,
            action.id(),
            MODULE_ID,
            gate);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Blog policy schemaVersion이 일치하지 않습니다.");
        require(BlogPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Blog policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Blog policy namespace가 일치하지 않습니다.");
        require(BlogPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Blog policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Blog policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(BlogPolicyAction.values())
            .map(BlogPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Blog policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
