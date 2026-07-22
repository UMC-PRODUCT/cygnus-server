package com.umc.product.recruiting.adapter.in.graphql.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitingApplicationAccessGraphQlRequestTest {

    @Test
    @DisplayName("applicationId와 credential 중 정확히 하나만 허용한다")
    void applicationId와_credential_중_정확히_하나만_허용한다() {
        var credential = new RecruitingApplicationCredentialGraphQlRequest("applicant@example.com", "secret");

        assertThat(new RecruitingApplicationAccessGraphQlRequest(1L, null).usesCredential()).isFalse();
        assertThat(new RecruitingApplicationAccessGraphQlRequest(null, credential).usesCredential()).isTrue();
        assertThatThrownBy(() -> new RecruitingApplicationAccessGraphQlRequest(null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingApplicationAccessGraphQlRequest(1L, credential))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("applicationId는 양수여야 한다")
    void applicationId는_양수여야_한다() {
        assertThatThrownBy(() -> new RecruitingApplicationAccessGraphQlRequest(0L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
