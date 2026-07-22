package com.umc.product.maintenance.adapter.in.web.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.global.response.ApiErrorResponseWriter;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;
import com.umc.product.maintenance.application.service.MaintenanceStateHolder;
import com.umc.product.maintenance.domain.MaintenanceDomain;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.maintenance.domain.MaintenanceSnapshot;
import com.umc.product.maintenance.exception.MaintenanceErrorCode;

import jakarta.servlet.FilterChain;

@DisplayName("MaintenanceFilter 인증 경계")
class MaintenanceFilterUnitTest {

    private static final Instant NOW = Instant.parse("2026-07-22T00:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비활성 점검은 일반 API 요청을 그대로 통과시킨다")
    void 비활성_점검은_요청을_통과시킨다() throws Exception {
        MaintenanceStateHolder stateHolder = mock(MaintenanceStateHolder.class);
        given(stateHolder.current()).willReturn(MaintenanceSnapshot.none());
        FilterChain chain = mock(FilterChain.class);
        MaintenanceFilter filter = filter(stateHolder, mock(ApiErrorResponseWriter.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notices/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        then(chain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("MemberPrincipal이 아닌 인증 principal은 점검 우회에 사용하지 않는다")
    void 비회원_principal은_점검을_우회하지_않는다() throws Exception {
        MaintenanceStateHolder stateHolder = mock(MaintenanceStateHolder.class);
        given(stateHolder.current()).willReturn(activeSnapshot());
        ApiErrorResponseWriter writer = mock(ApiErrorResponseWriter.class);
        MaintenanceFilter filter = filter(stateHolder, writer);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "service-client",
            "credential",
            List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(
            new MockHttpServletRequest("GET", "/api/v1/notices/1"),
            response,
            mock(FilterChain.class)
        );

        then(writer).should().write(
            org.mockito.ArgumentMatchers.same(response),
            org.mockito.ArgumentMatchers.eq(MaintenanceErrorCode.SERVICE_UNDER_MAINTENANCE),
            any()
        );
    }

    private MaintenanceFilter filter(MaintenanceStateHolder stateHolder, ApiErrorResponseWriter writer) {
        return new MaintenanceFilter(
            stateHolder,
            mock(MaintenanceBypassPolicy.class),
            writer,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private MaintenanceSnapshot activeSnapshot() {
        return new MaintenanceSnapshot(
            true,
            1L,
            MaintenanceScope.FULL,
            EnumSet.of(MaintenanceDomain.NOTICE),
            NOW.minusSeconds(60),
            NOW.plusSeconds(3600),
            "공지 점검",
            "점검 중"
        );
    }
}
