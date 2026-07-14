package com.umc.product.project.application.authorization;

public record ProjectPolicySurfaceIdentity(String id, String handler) {

    public ProjectPolicySurfaceIdentity {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Project 정책 호출면 ID는 비어 있을 수 없습니다.");
        }
        if (handler == null || handler.isBlank()) {
            throw new IllegalArgumentException("Project 정책 handler는 비어 있을 수 없습니다.");
        }
    }
}
