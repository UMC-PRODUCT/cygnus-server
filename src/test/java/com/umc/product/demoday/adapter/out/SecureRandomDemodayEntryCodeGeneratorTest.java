package com.umc.product.demoday.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecureRandomDemodayEntryCodeGeneratorTest {

    private final SecureRandomDemodayEntryCodeGenerator generator = new SecureRandomDemodayEntryCodeGenerator();

    @Test
    @DisplayName("GUEST- 접두어와 혼동 문자를 제외한 6자리로 코드를 생성한다")
    void generate() {
        // when
        String code = generator.generate();

        // then
        assertThat(code).startsWith("GUEST-");
        assertThat(code).matches("GUEST-[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");
    }
}
