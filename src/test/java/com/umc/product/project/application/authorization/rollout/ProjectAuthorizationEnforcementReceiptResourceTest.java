package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProjectAuthorizationEnforcementReceiptResourceTest {

    @Test
    @DisplayName("packaged receipt는 fixed classpath에 strict empty 문서로 존재한다")
    void packaged_receipt는_fixed_classpath에_strict_empty_문서로_존재한다() throws IOException {
        ClassPathResource resource = new ClassPathResource(
            "policies/project/rollout/enforcement-receipts.json");

        assertThat(resource.exists()).isTrue();

        ProjectAuthorizationEnforcementReceiptDocument document =
            new ProjectAuthorizationEnforcementReceiptJsonParser().parse(resource.getInputStream().readAllBytes());
        assertThat(document.schemaVersion()).isEqualTo("1.0");
        assertThat(document.receipts()).isEmpty();
    }
}
