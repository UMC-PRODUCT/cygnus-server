package com.umc.product.registry.domain;

import java.util.List;

public record RegistryCutoverPlan(List<RegistryCutoverMilestone> milestones) {

    private static final List<RegistryCutoverMilestone> REQUIRED_ORDER =
        List.of(RegistryCutoverMilestone.values());

    public RegistryCutoverPlan {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        if (!REQUIRED_ORDER.equals(milestones)) {
            throw new IllegalArgumentException("registry forward cutover milestone 순서가 유효하지 않습니다.");
        }
    }
}
