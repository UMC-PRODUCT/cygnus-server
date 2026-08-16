package com.umc.product.global.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.security.MemberPrincipal;

class ClientRequestClassifierTest {

    private final ClientRequestClassifier classifier = new ClientRequestClassifier(
        new ClientOriginRegistry(new ClientContextProperties(List.of(
            new ClientContextProperties.Origin(
                "https://university.neordinary.com",
                ClientServiceType.UMC_WEBSITE,
                ClientEnvironment.PROD
            ),
            new ClientContextProperties.Origin(
                "https://backoffice.university.neordinary.com",
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD
            ),
            new ClientContextProperties.Origin(
                "http://localhost:5173",
                ClientServiceType.UNKNOWN,
                ClientEnvironment.DEV
            )
        )))
    );

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(ClientRequestClassifierTestConfig.class);

    @Test
    @DisplayName("등록된 Origin 이 있으면 Origin 서비스와 환경을 우선하고 토큰 서비스 충돌은 mismatch 로 표시한다")
    void origin_우선_토큰_서비스_불일치() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("Origin", "https://university.neordinary.com");
        request.addHeader("User-Agent", desktopUserAgent());
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(1L)
            .clientType(ClientType.WEB)
            .clientContextClaims(ClientContextClaims.of(
                "backoffice",
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD
            ))
            .build();

        // when
        ClientRequestContext context = classifier.classify(request, principal);

        // then
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.UMC_WEBSITE);
        assertThat(context.environment()).isEqualTo(ClientEnvironment.PROD);
        assertThat(context.deviceType()).isEqualTo(ClientDeviceType.DESKTOP);
        assertThat(context.source()).isEqualTo("origin");
        assertThat(context.mismatched()).isTrue();
    }

    @Test
    @DisplayName("dev localhost Origin 은 환경만 DEV 로 판정하고 서비스는 토큰 claim 을 사용한다")
    void dev_localhost_서비스는_토큰_환경은_origin() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("User-Agent", desktopUserAgent());
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(1L)
            .clientType(ClientType.WEB)
            .clientContextClaims(ClientContextClaims.of(
                "website-dev",
                ClientServiceType.UMC_WEBSITE,
                ClientEnvironment.DEV
            ))
            .build();

        // when
        ClientRequestContext context = classifier.classify(request, principal);

        // then
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.UMC_WEBSITE);
        assertThat(context.environment()).isEqualTo(ClientEnvironment.DEV);
        assertThat(context.source()).isEqualTo("token");
        assertThat(context.mismatched()).isFalse();
    }

    @Test
    @DisplayName("dev profile은 localhost 5173 Origin 하나만 UNKNOWN 서비스와 DEV 환경으로 바인딩한다")
    void application_yml_dev_client_context_localhost_unknown_바인딩() {
        contextRunner
            .withPropertyValues("spring.profiles.active=dev")
            .run(context -> {
                ClientContextProperties properties = context.getBean(ClientContextProperties.class);

                assertThat(properties.origins()).hasSize(1);
                ClientContextProperties.Origin origin = properties.origins().getFirst();
                assertThat(origin.origin()).isEqualTo("http://localhost:5173");
                assertThat(origin.serviceType()).isEqualTo(ClientServiceType.UNKNOWN);
                assertThat(origin.environment()).isEqualTo(ClientEnvironment.DEV);
            });
    }

    @Test
    @DisplayName("Origin 이 없으면 Referer 에서 scheme host port 를 추출해 등록 Origin 을 판정한다")
    void referer_origin_대체_판정() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("Referer", "https://backoffice.university.neordinary.com/projects/1?tab=form");
        request.addHeader("User-Agent", desktopUserAgent());

        // when
        ClientRequestContext context = classifier.classify(request, null);

        // then
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.UMC_BACKOFFICE);
        assertThat(context.environment()).isEqualTo(ClientEnvironment.PROD);
        assertThat(context.source()).isEqualTo("origin");
        assertThat(context.mismatched()).isFalse();
    }

    @Test
    @DisplayName("ClientType IOS 는 User-Agent 보다 우선해 기기를 IOS 로 판정한다")
    void clientType_ios_기기_우선_판정() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("User-Agent", desktopUserAgent());
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(1L)
            .clientType(ClientType.IOS)
            .clientContextClaims(ClientContextClaims.of(
                "ios-app",
                ClientServiceType.IOS_APP,
                ClientEnvironment.PROD
            ))
            .build();

        // when
        ClientRequestContext context = classifier.classify(request, principal);

        // then
        assertThat(context.deviceType()).isEqualTo(ClientDeviceType.IOS);
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.IOS_APP);
        assertThat(context.source()).isEqualTo("token");
    }

    @Test
    @DisplayName("ClientType ANDROID 는 User-Agent 보다 우선해 기기를 ANDROID 로 판정한다")
    void clientType_android_기기_우선_판정() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("User-Agent", desktopUserAgent());
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(1L)
            .clientType(ClientType.ANDROID)
            .clientContextClaims(ClientContextClaims.of(
                "android-app",
                ClientServiceType.ANDROID_APP,
                ClientEnvironment.PROD
            ))
            .build();

        // when
        ClientRequestContext context = classifier.classify(request, principal);

        // then
        assertThat(context.deviceType()).isEqualTo(ClientDeviceType.ANDROID);
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.ANDROID_APP);
        assertThat(context.source()).isEqualTo("token");
    }

    @Test
    @DisplayName("WEB 또는 null ClientType 은 User-Agent 로 iOS Android Desktop 기기를 판정한다")
    void userAgent_기기_판정() {
        assertThat(classifier.classify(requestWithUserAgent(iosUserAgent()), null).deviceType())
            .isEqualTo(ClientDeviceType.IOS);
        assertThat(classifier.classify(requestWithUserAgent(androidUserAgent()), null).deviceType())
            .isEqualTo(ClientDeviceType.ANDROID);
        assertThat(classifier.classify(requestWithUserAgent(desktopUserAgent()), null).deviceType())
            .isEqualTo(ClientDeviceType.DESKTOP);
    }

    @Test
    @DisplayName("Origin 과 토큰 서비스가 모두 없으면 unknown 컨텍스트를 반환한다")
    void unknown_컨텍스트_반환() {
        // given
        MockHttpServletRequest request = request("GET", "/api/v1/forms");

        // when
        ClientRequestContext context = classifier.classify(request, null);

        // then
        assertThat(context.serviceType()).isEqualTo(ClientServiceType.UNKNOWN);
        assertThat(context.environment()).isEqualTo(ClientEnvironment.UNKNOWN);
        assertThat(context.deviceType()).isEqualTo(ClientDeviceType.UNKNOWN);
        assertThat(context.source()).isEqualTo("unknown");
        assertThat(context.mismatched()).isFalse();
    }

    private MockHttpServletRequest requestWithUserAgent(String userAgent) {
        MockHttpServletRequest request = request("GET", "/api/v1/forms");
        request.addHeader("User-Agent", userAgent);
        return request;
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private String iosUserAgent() {
        return "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15";
    }

    private String androidUserAgent() {
        return "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36";
    }

    private String desktopUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 Chrome/126 Safari/537.36";
    }

    @Configuration
    @EnableConfigurationProperties(ClientContextProperties.class)
    static class ClientRequestClassifierTestConfig {
    }
}
