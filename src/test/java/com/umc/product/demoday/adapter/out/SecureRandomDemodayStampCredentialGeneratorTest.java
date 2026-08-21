package com.umc.product.demoday.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecureRandomDemodayStampCredentialGeneratorTest {

    private final SecureRandomDemodayStampCredentialGenerator generator = new SecureRandomDemodayStampCredentialGenerator();

    @Test
    @DisplayName("44바이트 난수를 패딩 없는 Base64 URL 문자열로 생성한다")
    void generate() {
        // when
        String credential = generator.generate();

        // then
        assertThat(credential).doesNotContain("=");
        assertThat(Base64.getUrlDecoder().decode(credential)).hasSize(44);
    }
}
