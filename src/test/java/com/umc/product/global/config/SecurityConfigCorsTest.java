package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * SecurityConfig 의 CORS 오리진 정책 단위 테스트.
 * <p>
 * 소유 도메인(university.neordinary.com)은 프로필별로 서브도메인까지 통째로 허용하고, 그 밖의 오리진은 환경변수로 지정한 패턴만 허용하는지 확인한다.
 */
@DisplayName("SecurityConfig CORS 오리진 정책")
class SecurityConfigCorsTest {

    private static final List<String> PROD_TRUSTED = List.of(
        "https://university.neordinary.com",
        "https://*.university.neordinary.com"
    );

    private static final List<String> ALPHA_TRUSTED = List.of(
        "https://alpha.university.neordinary.com",
        "https://*.alpha.university.neordinary.com"
    );

    private static CorsConfiguration corsConfiguration(List<String> trusted, List<String> configured) {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
        ReflectionTestUtils.setField(securityConfig, "trustedOriginPatterns", trusted);
        ReflectionTestUtils.setField(securityConfig, "allowedOriginPatterns", configured);

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        return configuration;
    }

    @Nested
    @DisplayName("prod 프로필")
    class Prod {

        private final CorsConfiguration configuration = corsConfiguration(PROD_TRUSTED, List.of());

        @Test
        @DisplayName("소유 도메인의 서브도메인은 별도 등록 없이 모두 허용한다")
        void 소유_도메인_서브도메인은_모두_허용된다() {
            // given
            String origin = "https://backoffice.university.neordinary.com";

            // when
            String allowed = configuration.checkOrigin(origin);

            // then
            assertThat(allowed).isEqualTo(origin);
        }

        @Test
        @DisplayName("소유 도메인의 apex 오리진도 허용한다")
        void apex_오리진도_허용된다() {
            // given
            String origin = "https://university.neordinary.com";

            // when & then
            assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        }

        @Test
        @DisplayName("중첩 서브도메인도 허용한다")
        void 중첩_서브도메인도_허용된다() {
            // given
            String origin = "https://tech.blog.university.neordinary.com";

            // when & then
            assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        }

        @Test
        @DisplayName("소유 도메인이 아닌 오리진은 차단한다")
        void 외부_오리진은_차단된다() {
            // when & then
            assertThat(configuration.checkOrigin("https://evil.com")).isNull();
            assertThat(configuration.checkOrigin("https://university.neordinary.com.evil.com")).isNull();
            assertThat(configuration.checkOrigin("https://evil-university.neordinary.com")).isNull();
        }

        @Test
        @DisplayName("http 스킴은 허용하지 않는다")
        void http_스킴은_차단된다() {
            // when & then
            assertThat(configuration.checkOrigin("http://backoffice.university.neordinary.com")).isNull();
        }
    }

    @Nested
    @DisplayName("alpha 프로필")
    class Alpha {

        private final CorsConfiguration configuration = corsConfiguration(ALPHA_TRUSTED, List.of());

        @Test
        @DisplayName("alpha 소유 도메인의 서브도메인은 모두 허용한다")
        void alpha_서브도메인은_모두_허용된다() {
            // given
            String origin = "https://admin.alpha.university.neordinary.com";

            // when & then
            assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        }

        @Test
        @DisplayName("alpha apex 오리진도 허용한다")
        void alpha_apex_오리진도_허용된다() {
            // given
            String origin = "https://alpha.university.neordinary.com";

            // when & then
            assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        }

        @Test
        @DisplayName("prod 오리진은 alpha 환경에서 허용하지 않는다")
        void prod_오리진은_차단된다() {
            // when & then
            assertThat(configuration.checkOrigin("https://backoffice.university.neordinary.com")).isNull();
        }
    }

    @Nested
    @DisplayName("환경변수로 추가한 오리진")
    class ConfiguredOrigins {

        @Test
        @DisplayName("소유 도메인 패턴과 환경변수 패턴을 함께 허용한다")
        void 소유_도메인과_환경변수_패턴을_모두_허용한다() {
            // given
            CorsConfiguration configuration = corsConfiguration(PROD_TRUSTED, List.of("http://localhost:5173"));

            // when & then
            assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
            assertThat(configuration.checkOrigin("https://backoffice.university.neordinary.com"))
                .isEqualTo("https://backoffice.university.neordinary.com");
        }

        @Test
        @DisplayName("소유 도메인 설정이 비어 있으면 환경변수 패턴만 허용한다")
        void 소유_도메인_설정이_비면_환경변수_패턴만_허용한다() {
            // given (local/test 프로필처럼 trusted 값이 빈 문자열로 주입되는 경우)
            CorsConfiguration configuration = corsConfiguration(List.of(""), List.of("http://localhost:5173"));

            // when & then
            assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
            assertThat(configuration.checkOrigin("https://backoffice.university.neordinary.com")).isNull();
        }

        @Test
        @DisplayName("빈 값과 중복 패턴은 제거하고 순서를 유지한다")
        void 빈_값과_중복은_제거된다() {
            // when
            List<String> merged = SecurityConfig.mergeOriginPatterns(
                List.of("https://university.neordinary.com", " ", "https://*.university.neordinary.com"),
                List.of("https://university.neordinary.com", "http://localhost:5173", "")
            );

            // then
            assertThat(merged).containsExactly(
                "https://university.neordinary.com",
                "https://*.university.neordinary.com",
                "http://localhost:5173"
            );
        }

        @Test
        @DisplayName("설정이 모두 비어 있으면 어떤 오리진도 허용하지 않는다")
        void 설정이_모두_비면_모든_오리진을_차단한다() {
            // given
            CorsConfiguration configuration = corsConfiguration(List.of(""), List.of());

            // when & then
            assertThat(configuration.checkOrigin("https://backoffice.university.neordinary.com")).isNull();
            assertThat(configuration.checkOrigin("http://localhost:5173")).isNull();
        }
    }

    @Nested
    @DisplayName("application.yml 프로필별 소유 도메인 설정")
    class TrustedOriginPatternsByProfile {

        private static String trustedOriginPatterns(String profile) {
            AtomicReference<String> value = new AtomicReference<>();
            new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> value.set(
                    context.getEnvironment().getProperty("app.cors.trusted-origin-patterns")
                ));
            return value.get();
        }

        @Test
        @DisplayName("prod 프로필은 university.neordinary.com 전체를 허용한다")
        void prod_프로필_소유_도메인() {
            assertThat(trustedOriginPatterns("prod"))
                .isEqualTo("https://university.neordinary.com,https://*.university.neordinary.com");
        }

        @Test
        @DisplayName("alpha 프로필은 alpha.university.neordinary.com 전체를 허용한다")
        void alpha_프로필_소유_도메인() {
            assertThat(trustedOriginPatterns("alpha"))
                .isEqualTo("https://alpha.university.neordinary.com,https://*.alpha.university.neordinary.com");
        }

        @Test
        @DisplayName("local 프로필은 소유 도메인을 자동 허용하지 않는다")
        void local_프로필은_비어있다() {
            assertThat(trustedOriginPatterns("local")).isEmpty();
        }
    }
}
