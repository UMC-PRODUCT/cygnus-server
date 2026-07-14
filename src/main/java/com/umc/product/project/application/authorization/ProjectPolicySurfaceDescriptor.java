package com.umc.product.project.application.authorization;

import java.util.Objects;

public record ProjectPolicySurfaceDescriptor(
    ProjectPolicySurfaceIdentity identity,
    ProjectPolicySurfaceType type,
    ProjectPolicyAction action,
    ProjectPolicyModule module,
    ProjectPolicyGate gate
) {

    public ProjectPolicySurfaceDescriptor {
        Objects.requireNonNull(identity, "Project 정책 호출면 identity는 필수입니다.");
        Objects.requireNonNull(type, "Project 정책 호출면 type은 필수입니다.");
        Objects.requireNonNull(action, "Project 정책 호출면 action은 필수입니다.");
        Objects.requireNonNull(module, "Project 정책 호출면 module은 필수입니다.");
        Objects.requireNonNull(gate, "Project 정책 호출면 gate는 필수입니다.");
    }

    public String id() {
        return identity.id();
    }

    public String handler() {
        return identity.handler();
    }
}
