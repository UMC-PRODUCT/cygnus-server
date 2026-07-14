package com.umc.product.project.application.authorization;

import java.util.List;

final class ProjectPolicyResourceManifest {

    static final PolicyResource BUNDLE = new PolicyResource(
        "bundle.json",
        "policies/project/bundle.json"
    );

    static final List<PolicyResource> MODULES = List.of(
        new PolicyResource("project-resource.policy.json", "policies/project/project-resource.policy.json"),
        new PolicyResource("project-scope.policy.json", "policies/project/project-scope.policy.json"),
        new PolicyResource("application-resource.policy.json", "policies/project/application-resource.policy.json"),
        new PolicyResource("application-scope.policy.json", "policies/project/application-scope.policy.json"),
        new PolicyResource("form.policy.json", "policies/form/form.policy.json"),
        new PolicyResource("statistics.policy.json", "policies/project/statistics.policy.json"),
        new PolicyResource("matching-round.policy.json", "policies/project/matching-round.policy.json")
    );

    static final List<PolicyResource> POLICIES = java.util.stream.Stream.concat(
        java.util.stream.Stream.of(BUNDLE),
        MODULES.stream()
    ).toList();

    private ProjectPolicyResourceManifest() {
    }

    record PolicyResource(String logicalFilename, String classpathPath) {
    }
}
