package com.umc.product.authentication.adapter.out.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;

class SsoClientConfigAdapterTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(SsoClientConfigAdapterTestConfig.class);

    @DisplayName("SSO client는 redirect URI와 client context를 yml에서 로드한다")
    @Test
    void sso_client_registry_로드_성공() {
        SsoProperties properties = new SsoProperties(
            URI.create("https://api.university.neordinary.com"),
            Duration.ofMinutes(3),
            Duration.ofHours(12),
            new SsoProperties.Cookie("UMC_SSO_LOGIN", ".university.neordinary.com", true, "Lax"),
            Map.of("backoffice", new SsoProperties.Client(
                "UMC Backoffice",
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD,
                true,
                Duration.ofHours(1),
                List.of("https://backoffice.university.neordinary.com/auth/callback"),
                List.of("https://backoffice.university.neordinary.com")
            ))
        );
        SsoClientConfigAdapter adapter = new SsoClientConfigAdapter(properties);

        SsoClient client = adapter.getByClientId("backoffice");

        assertThat(client.clientId()).isEqualTo("backoffice");
        assertThat(client.serviceType()).isEqualTo(ClientServiceType.UMC_BACKOFFICE);
        assertThat(client.environment()).isEqualTo(ClientEnvironment.PROD);
        assertThat(client.allowsRedirectUri("https://backoffice.university.neordinary.com/auth/callback")).isTrue();
        assertThat(client.allowsRedirectUri("https://evil.example.com/auth/callback")).isFalse();
        assertThat(client.allowsOrigin("https://backoffice.university.neordinary.com")).isTrue();
        assertThat(client.allowsOrigin("https://evil.example.com")).isFalse();
    }

    @DisplayName("SSO client 생성자는 기본값과 리스트 불변성을 보장한다")
    @Test
    void sso_client_canonical_constructor_불변식_보장() {
        List<String> redirectUris = new ArrayList<>(List.of("https://university.neordinary.com/auth/callback"));
        List<String> allowedOrigins = new ArrayList<>(List.of("https://university.neordinary.com"));

        SsoClient client = new SsoClient(
            "website",
            "UMC Website",
            null,
            null,
            true,
            Duration.ofHours(1),
            redirectUris,
            allowedOrigins
        );

        redirectUris.add("https://evil.example.com/auth/callback");
        allowedOrigins.add("https://evil.example.com");

        assertThat(client.serviceType()).isEqualTo(ClientServiceType.UNKNOWN);
        assertThat(client.environment()).isEqualTo(ClientEnvironment.UNKNOWN);
        assertThat(client.redirectUris()).containsExactly("https://university.neordinary.com/auth/callback");
        assertThat(client.allowedOrigins()).containsExactly("https://university.neordinary.com");
        assertThatThrownBy(() -> client.redirectUris().add("https://another.example.com/auth/callback"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @DisplayName("SSO client 생성자는 잘못된 redirect URI와 origin을 거부한다")
    @Test
    void sso_client_canonical_constructor_잘못된_리스트_거부() {
        assertThatThrownBy(() -> new SsoClient(
            "website",
            "UMC Website",
            ClientServiceType.UMC_WEBSITE,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of(" "),
            List.of("https://university.neordinary.com")
        ))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_REDIRECT_URI);

        assertThatThrownBy(() -> new SsoClient(
            "website",
            "UMC Website",
            ClientServiceType.UMC_WEBSITE,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of("https://university.neordinary.com/auth/callback"),
            List.of(" ")
        ))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_CLIENT);
    }

    @DisplayName("Client context claims 생성자는 null context를 UNKNOWN으로 보정한다")
    @Test
    void client_context_claims_canonical_constructor_null_context_보정() {
        ClientContextClaims claims = new ClientContextClaims("client", null, null);

        assertThat(claims.clientId()).isEqualTo("client");
        assertThat(claims.serviceType()).isEqualTo(ClientServiceType.UNKNOWN);
        assertThat(claims.environment()).isEqualTo(ClientEnvironment.UNKNOWN);
    }

    @DisplayName("누락된 SSO client 조회는 예외를 던진다")
    @Test
    void sso_client_registry_누락_client_예외() {
        SsoProperties properties = new SsoProperties(
            URI.create("https://api.university.neordinary.com"),
            Duration.ofMinutes(3),
            Duration.ofHours(12),
            new SsoProperties.Cookie("UMC_SSO_LOGIN", ".university.neordinary.com", true, "Lax"),
            Map.of()
        );
        SsoClientConfigAdapter adapter = new SsoClientConfigAdapter(properties);

        assertThatThrownBy(() -> adapter.getByClientId("missing"))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_CLIENT);
    }

    @DisplayName("기본 설정은 website client의 prod redirect URI와 origin을 바인딩한다")
    @Test
    void application_yml_base_sso_client_binding_성공() {
        contextRunner.run(context -> {
            SsoClientConfigAdapter adapter = context.getBean(SsoClientConfigAdapter.class);

            SsoClient website = adapter.getByClientId("website");

            assertThat(website.environment()).isEqualTo(ClientEnvironment.PROD);
            assertThat(website.redirectUris())
                .containsExactly("https://university.neordinary.com/auth/callback")
                .doesNotContain("https://www.university.neordinary.com/auth/callback");
            assertThat(website.allowedOrigins())
                .containsExactly("https://university.neordinary.com")
                .doesNotContain("https://www.university.neordinary.com");
        });
    }

    @DisplayName("dev profile은 웹 client를 localhost로 덮어쓰고 앱 client는 기본 설정을 유지한다")
    @Test
    void application_yml_dev_sso_client_binding_성공() {
        contextRunner
            .withPropertyValues("spring.profiles.active=dev")
            .run(context -> {
                SsoClientConfigAdapter adapter = context.getBean(SsoClientConfigAdapter.class);

                assertDevWebClient(
                    adapter.getByClientId("backoffice"),
                    "https://dev.admin.university.neordinary.com/auth/callback",
                    "https://dev.admin.university.neordinary.com"
                );
                assertDevWebClient(
                    adapter.getByClientId("website"),
                    "https://dev.university.neordinary.com/auth/callback",
                    "https://dev.university.neordinary.com"
                );
                assertDevWebClient(
                    adapter.getByClientId("tech"),
                    "https://dev.tech.university.neordinary.com/auth/callback",
                    "https://dev.tech.university.neordinary.com"
                );

                SsoClient iosApp = adapter.getByClientId("ios-app");
                SsoClient androidApp = adapter.getByClientId("android-app");

                assertThat(iosApp.serviceType()).isEqualTo(ClientServiceType.IOS_APP);
                assertThat(iosApp.environment()).isEqualTo(ClientEnvironment.PROD);
                assertThat(iosApp.redirectUris()).contains("umc-ios://auth/callback");
                assertThat(iosApp.allowedOrigins()).isEmpty();
                assertThat(androidApp.serviceType()).isEqualTo(ClientServiceType.ANDROID_APP);
                assertThat(androidApp.environment()).isEqualTo(ClientEnvironment.PROD);
                assertThat(androidApp.redirectUris()).contains("umc-android://auth/callback");
                assertThat(androidApp.allowedOrigins()).isEmpty();
            });
    }

    private void assertDevWebClient(SsoClient client, String redirectUri, String origin) {
        assertThat(client.environment()).isEqualTo(ClientEnvironment.DEV);
        assertThat(client.redirectUris()).containsExactly(redirectUri);
        assertThat(client.allowedOrigins()).containsExactly(origin);
    }

    @Configuration
    @EnableConfigurationProperties(SsoProperties.class)
    static class SsoClientConfigAdapterTestConfig {

        @Bean
        SsoClientConfigAdapter ssoClientConfigAdapter(SsoProperties properties) {
            return new SsoClientConfigAdapter(properties);
        }
    }
}
