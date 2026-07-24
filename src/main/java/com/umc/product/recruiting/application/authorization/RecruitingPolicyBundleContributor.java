package com.umc.product.recruiting.application.authorization;

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
public class RecruitingPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "recruiting-resource";

    @Override
    public String namespace() {
        return RecruitingPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return RecruitingPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/recruiting/bundle.json"),
            List.of(new PolicyClasspathResource(
                "recruiting-resource.policy.json",
                "policies/recruiting/recruiting-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            internal(
                "capability:ResourceType.RECRUITMENT/READ-WRITE-EDIT-APPROVE",
                "com.umc.product.recruiting.application.service.evaluator."
                    + "RecruitingPermissionEvaluator#evaluate",
                RecruitingPolicyAction.OPERATE_SCHOOL,
                PolicySurfaceGate.CAPABILITY),
            internal(
                "capability:ResourceType.RECRUITMENT/MANAGE",
                "com.umc.product.recruiting.application.service.evaluator."
                    + "RecruitingPermissionEvaluator#evaluate",
                RecruitingPolicyAction.MANAGE_ALL,
                PolicySurfaceGate.CAPABILITY),
            rest(
                "POST /api/v1/recruiting/admin/seasons",
                "RecruitingSeasonAdminController#createSeason",
                RecruitingPolicyAction.SEASON_CREATE),
            rest(
                "PATCH /api/v1/recruiting/admin/applications/{applicationId}/document-decision",
                "RecruitingAdminController#decideDocument",
                RecruitingPolicyAction.APPLICATION_DECIDE),
            rest(
                "POST /api/v1/recruiting/admin/applications/{applicationId}/interview/skip",
                "RecruitingAdminController#skipInterview",
                RecruitingPolicyAction.APPLICATION_DECIDE),
            rest(
                "PATCH /api/v1/recruiting/admin/applications/{applicationId}/final-decision",
                "RecruitingAdminController#decideFinal",
                RecruitingPolicyAction.APPLICATION_DECIDE),
            rest(
                "POST /api/v1/recruiting/admin/applications/{applicationId}/registration/ready",
                "RecruitingAdminController#prepareRegistration",
                RecruitingPolicyAction.REGISTRATION_MANAGE),
            rest(
                "DELETE /api/v1/recruiting/admin/applications/{applicationId}/registration/ready",
                "RecruitingAdminController#cancelRegistration",
                RecruitingPolicyAction.REGISTRATION_MANAGE),
            rest(
                "POST /api/v1/recruiting/admin/applications/{applicationId}/registration/registered",
                "RecruitingAdminController#confirmRegistration",
                RecruitingPolicyAction.REGISTRATION_MANAGE),
            rest(
                "GET /api/v1/recruiting/admin/summary",
                "RecruitingAdminController#getSummary",
                RecruitingPolicyAction.SUMMARY_READ),
            rest(
                "GET /api/v1/recruiting/admin/statistics.csv",
                "RecruitingAdminController#exportCsv",
                RecruitingPolicyAction.CSV_EXPORT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        RecruitingPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.recruiting.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private PolicySurfaceDescriptor internal(
        String id,
        String handler,
        RecruitingPolicyAction action,
        PolicySurfaceGate gate
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            id,
            handler,
            PolicySurfaceType.INTERNAL_BATCH,
            action.id(),
            MODULE_ID,
            gate);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Recruiting policy schemaVersion이 일치하지 않습니다.");
        require(RecruitingPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Recruiting policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Recruiting policy namespace가 일치하지 않습니다.");
        require(RecruitingPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Recruiting policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Recruiting policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(RecruitingPolicyAction.values())
            .map(RecruitingPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Recruiting policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
