package com.umc.product.recruiting.adapter.out.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.security.SecureRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitingApplicationKeyGeneratorTest {

    @Test
    @DisplayName("SecureRandom 지원 키는 대문자와 숫자로 된 6자리다")
    void generateSixUppercaseAlphanumericCharacters() {
        RecruitingApplicationKeyGenerator generator = new RecruitingApplicationKeyGenerator();

        assertThat(IntStream.range(0, 100).mapToObj(ignored -> generator.generate()))
            .allMatch(key -> key.matches("[A-Z0-9]{6}"));
    }

    @Test
    @DisplayName("지원 키 생성기는 SecureRandom에서 정확히 6개 문자를 선택한다")
    void generateFromSecureRandom() {
        SecureRandom secureRandom = mock(SecureRandom.class);
        given(secureRandom.nextInt(36)).willReturn(0, 25, 26, 35, 1, 27);
        RecruitingApplicationKeyGenerator generator = new RecruitingApplicationKeyGenerator(secureRandom);

        String generated = generator.generate();

        assertThat(generated).isEqualTo("AZ09B1");
        then(secureRandom).should(times(6)).nextInt(36);
    }
}
