package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientContextProperties;
import com.umc.product.global.client.ClientDeviceType;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientOriginRegistry;
import com.umc.product.global.client.ClientRequestClassifier;
import com.umc.product.global.client.ClientRequestContext;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class LoggingInterceptorClientContextTest {

    private static final String CLIENT_REQUEST_CONTEXT_ATTR = "clientRequestContext";

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("preHandle 은 클라이언트 컨텍스트를 request attribute 와 MDC 에 저장한다")
    void preHandle_클라이언트_컨텍스트_MDC_등록() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoggingInterceptor interceptor = interceptor(registry);
        authenticate(MemberPrincipal.builder()
            .memberId(42L)
            .clientType(ClientType.WEB)
            .clientContextClaims(ClientContextClaims.of(
                "website",
                ClientServiceType.UMC_WEBSITE,
                ClientEnvironment.PROD
            ))
            .build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/forms");
        request.addHeader("Origin", "https://university.neordinary.com");
        request.addHeader("User-Agent", desktopUserAgent());
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(CLIENT_REQUEST_CONTEXT_ATTR))
            .isEqualTo(new ClientRequestContext(
                ClientServiceType.UMC_WEBSITE,
                ClientDeviceType.DESKTOP,
                ClientEnvironment.PROD,
                "origin",
                false
            ));
        assertThat(MDC.get("clientService")).isEqualTo("UMC_WEBSITE");
        assertThat(MDC.get("clientDevice")).isEqualTo("DESKTOP");
        assertThat(MDC.get("clientEnvironment")).isEqualTo("PROD");
        assertThat(MDC.get("clientContextSource")).isEqualTo("origin");
        assertThat(MDC.get("clientType")).isEqualTo("WEB");
    }

    @Test
    @DisplayName("afterCompletion 은 클라이언트 요청 메트릭을 service device environment source statusFamily 태그로 기록한다")
    void afterCompletion_클라이언트_요청_메트릭_기록() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoggingInterceptor interceptor = interceptor(registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/forms");
        request.addHeader("Origin", "https://university.neordinary.com");
        request.addHeader("User-Agent", desktopUserAgent());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        interceptor.preHandle(request, response, new Object());

        // when
        interceptor.afterCompletion(request, response, new Object(), null);

        // then
        assertThat(registry.get("operational.client.request.total")
            .tag("service", "UMC_WEBSITE")
            .tag("device", "DESKTOP")
            .tag("environment", "PROD")
            .tag("source", "origin")
            .tag("statusFamily", "2xx")
            .counter()
            .count()).isEqualTo(1);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Origin 과 토큰 서비스가 충돌하면 보안 이벤트 메트릭을 기록한다")
    void mismatch_보안_이벤트_메트릭_기록() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoggingInterceptor interceptor = interceptor(registry);
        authenticate(MemberPrincipal.builder()
            .memberId(42L)
            .clientType(ClientType.WEB)
            .clientContextClaims(ClientContextClaims.of(
                "backoffice",
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD
            ))
            .build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/forms");
        request.addHeader("Origin", "https://university.neordinary.com");
        request.addHeader("User-Agent", desktopUserAgent());
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        // then
        assertThat(registry.get("operational.security.event.total")
            .tag("domain", "AUTHENTICATION")
            .tag("operation", "CLIENT_CONTEXT_MISMATCH")
            .tag("result", "detected")
            .counter()
            .count()).isEqualTo(1);
    }

    private LoggingInterceptor interceptor(SimpleMeterRegistry registry) {
        ClientContextProperties properties = new ClientContextProperties(List.of(
            new ClientContextProperties.Origin(
                "https://university.neordinary.com",
                ClientServiceType.UMC_WEBSITE,
                ClientEnvironment.PROD
            )
        ));
        return new LoggingInterceptor(
            new ClientRequestClassifier(new ClientOriginRegistry(properties)),
            new OperationalMetrics(registry)
        );
    }

    private void authenticate(MemberPrincipal principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String desktopUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 Chrome/126 Safari/537.36";
    }
}
