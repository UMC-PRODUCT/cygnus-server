package com.umc.product.recruiting.adapter.in.graphql.dto;

public record RecruitingApplicationAccessGraphQlRequest(
    Long applicationId,
    RecruitingApplicationCredentialGraphQlRequest credential
) {

    public RecruitingApplicationAccessGraphQlRequest {
        if ((applicationId == null) == (credential == null)) {
            throw new IllegalArgumentException("applicationId와 credential 중 정확히 하나만 입력해야 합니다.");
        }
        if (applicationId != null && applicationId <= 0) {
            throw new IllegalArgumentException("applicationId는 양수여야 합니다.");
        }
    }

    public boolean usesCredential() {
        return credential != null;
    }
}
