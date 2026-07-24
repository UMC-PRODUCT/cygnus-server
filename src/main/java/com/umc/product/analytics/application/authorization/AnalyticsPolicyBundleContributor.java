package com.umc.product.analytics.application.authorization;

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
public class AnalyticsPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "analytics-resource";
    private static final String DASHBOARD_CONTROLLER =
        "com.umc.product.analytics.adapter.in.web.AdminDashboardController#";

    @Override
    public String namespace() {
        return AnalyticsPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return AnalyticsPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/analytics/bundle.json"),
            List.of(new PolicyClasspathResource(
                "analytics-resource.policy.json",
                "policies/analytics/analytics-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            dashboard("GET /api/v1/analytics/admin/dashboard/summary", "getSummary"),
            dashboard("GET /api/v1/analytics/admin/dashboard/action-queue", "getActionQueue"),
            dashboard("GET /api/v1/analytics/admin/dashboard/context", "getContext"),
            dashboard("GET /api/v1/analytics/admin/dashboard/operations", "getOperationsOverview"),
            dashboard("GET /api/v1/analytics/admin/dashboard/operations/schools", "getOperationsSchools"),
            dashboard("GET /api/v1/analytics/admin/dashboard/operations/points", "getOperationsPoints"),
            dashboard("GET /api/v1/analytics/admin/dashboard/operations/attendance", "getOperationsAttendance"),
            dashboard(
                "GET /api/v1/analytics/admin/dashboard/operations/study-groups",
                "getOperationsStudyGroups"),
            dashboard("GET /api/v1/analytics/admin/dashboard/operations/signups", "getOperationsSignups"),
            dashboard("GET /api/v1/analytics/admin/dashboard/risk-challengers", "getRiskChallengers"),
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:GET /api/v1/analytics/admin/schools/summary",
                "com.umc.product.analytics.adapter.in.web.AdminSchoolAnalyticsController#getSchoolSummaries",
                PolicySurfaceType.REST,
                AnalyticsPolicyAction.READ_SCHOOL.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor dashboard(String route, String handler) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            DASHBOARD_CONTROLLER + handler,
            PolicySurfaceType.REST,
            AnalyticsPolicyAction.READ_DASHBOARD.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Analytics policy schemaVersion이 일치하지 않습니다.");
        require(AnalyticsPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Analytics policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Analytics policy namespace가 일치하지 않습니다.");
        require(AnalyticsPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Analytics policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Analytics policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(AnalyticsPolicyAction.values())
            .map(AnalyticsPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Analytics policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
