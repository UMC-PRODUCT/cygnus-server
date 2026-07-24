package com.umc.product.schedule.application.authorization;

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
public class SchedulePolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "schedule-resource";

    @Override
    public String namespace() {
        return SchedulePolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return SchedulePolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/schedule/bundle.json"),
            List.of(new PolicyClasspathResource(
                "schedule-resource.policy.json",
                "policies/schedule/schedule-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest("GET /api/v2/schedules/me", "ScheduleQueryV2Controller#mySchedules",
                SchedulePolicyAction.SCHEDULE_READ),
            rest("GET /api/v2/schedules/{scheduleId}", "ScheduleQueryV2Controller#details",
                SchedulePolicyAction.SCHEDULE_READ),
            rest("POST /api/v2/schedules", "ScheduleCommandV2Controller#create",
                SchedulePolicyAction.SCHEDULE_CREATE),
            rest("PATCH /api/v2/schedules/{scheduleId}", "ScheduleCommandV2Controller#edit",
                SchedulePolicyAction.SCHEDULE_UPDATE),
            rest("DELETE /api/v2/schedules/{scheduleId}", "ScheduleCommandV2Controller#delete",
                SchedulePolicyAction.SCHEDULE_DELETE),
            rest("DELETE /api/v2/schedules/{scheduleId}/force",
                "ScheduleCommandV2Controller#forceDelete",
                SchedulePolicyAction.SCHEDULE_FORCE_DELETE),
            rest("POST /api/v2/schedules/{scheduleId}/attendances/request",
                "ScheduleCommandV2Controller#requestAttendance",
                SchedulePolicyAction.ATTENDANCE_SUBMIT),
            rest("POST /api/v2/schedules/{scheduleId}/attendances/excuse",
                "ScheduleCommandV2Controller#excuseAttendance",
                SchedulePolicyAction.ATTENDANCE_SUBMIT),
            rest("GET /api/v2/schedules/attendance",
                "ScheduleQueryV2Controller#getAttendanceInfoList",
                SchedulePolicyAction.ATTENDANCE_READ),
            rest("GET /api/v2/schedules/{scheduleId}/attendance",
                "ScheduleQueryV2Controller#getAttendanceInfo",
                SchedulePolicyAction.ATTENDANCE_READ),
            rest("POST /api/v2/schedules/{scheduleId}/attendances/decide",
                "ScheduleCommandV2Controller#decideAttendances",
                SchedulePolicyAction.ATTENDANCE_APPROVE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        SchedulePolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.schedule.adapter.in.web.v2." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Schedule policy schemaVersion이 일치하지 않습니다.");
        require(SchedulePolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Schedule policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Schedule policy namespace가 일치하지 않습니다.");
        require(SchedulePolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Schedule policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Schedule policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(SchedulePolicyAction.values())
            .map(SchedulePolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Schedule policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
