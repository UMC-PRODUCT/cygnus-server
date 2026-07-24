package com.umc.product.challenger.application.authorization;

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
public class ChallengerPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "challenger-resource";

    @Override
    public String namespace() {
        return ChallengerPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return ChallengerPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/challenger/bundle.json"),
            List.of(new PolicyClasspathResource(
                "challenger-resource.policy.json",
                "policies/challenger/challenger-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest("POST /api/v1/challengers", "ChallengerCommandController#createChallenger",
                ChallengerPolicyAction.CHALLENGER_CREATE),
            rest("POST /api/v1/challengers/batch", "ChallengerCommandController#bulkCreateChallenger",
                ChallengerPolicyAction.CHALLENGER_CREATE),
            rest("POST /api/v1/challengers/{challengerId}/deactivate",
                "ChallengerCommandController#deactivateChallenger",
                ChallengerPolicyAction.CHALLENGER_UPDATE),
            rest("PATCH /api/v1/challengers/{challengerId}/part",
                "ChallengerCommandController#editChallengerInfo",
                ChallengerPolicyAction.CHALLENGER_UPDATE),
            rest("DELETE /api/v1/challengers/{challengerId}",
                "ChallengerCommandController#deleteChallenger",
                ChallengerPolicyAction.CHALLENGER_DELETE),
            rest("POST /api/v1/challengers/{challengerId}/points",
                "ChallengerPointCommandController#grantChallengerPoints",
                ChallengerPolicyAction.POINT_CREATE),
            rest("PATCH /api/v1/challengers/points/{challengerPointId}",
                "ChallengerPointCommandController#editChallengerPoints",
                ChallengerPolicyAction.POINT_UPDATE),
            rest("DELETE /api/v1/challengers/points/{challengerPointId}",
                "ChallengerPointCommandController#deleteChallengerPoint",
                ChallengerPolicyAction.POINT_DELETE),
            rest("GET /api/v1/challenger-records/code/{code}",
                "ChallengerRecordController#getChallengerRecordByCode",
                ChallengerPolicyAction.RECORD_READ),
            rest("GET /api/v1/challenger-records/id/{id}",
                "ChallengerRecordController#getChallengerRecordById",
                ChallengerPolicyAction.RECORD_READ),
            rest("POST /api/v1/challenger-records",
                "ChallengerRecordController#createChallengerRecord",
                ChallengerPolicyAction.RECORD_CREATE),
            rest("POST /api/v1/challenger-records/bulk",
                "ChallengerRecordController#createChallengerRecordBulk",
                ChallengerPolicyAction.RECORD_CREATE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        ChallengerPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.challenger.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Challenger policy schemaVersion이 일치하지 않습니다.");
        require(ChallengerPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Challenger policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Challenger policy namespace가 일치하지 않습니다.");
        require(ChallengerPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Challenger policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Challenger policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(ChallengerPolicyAction.values())
            .map(ChallengerPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Challenger policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
