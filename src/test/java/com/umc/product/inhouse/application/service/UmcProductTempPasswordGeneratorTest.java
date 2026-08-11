package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import com.umc.product.authentication.domain.CredentialPolicy;

class UmcProductTempPasswordGeneratorTest {

    private final UmcProductTempPasswordGenerator generator = new UmcProductTempPasswordGenerator();

    @Test
    void 생성된_임시_비밀번호는_자격증명_정책을_통과한다() {
        for (int index = 0; index < 100; index++) {
            String password = generator.generate();
            assertThat(password).hasSize(16).doesNotContainAnyWhitespaces();
            assertThatCode(() -> CredentialPolicy.validatePassword(password)).doesNotThrowAnyException();
        }
    }
}
