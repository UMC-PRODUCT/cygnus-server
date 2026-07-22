package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.umc.product.global.security.ApiAccessDeniedHandler;
import com.umc.product.global.security.ApiAuthenticationEntryPoint;
import com.umc.product.global.security.JwtAuthenticationFilter;

class SecurityConfigResidualTest {

    @Test
    @DisplayName("dummy UserDetailsService는 password 인증을 시도하면 명시적으로 거부한다")
    void dummy_user_details_service_rejects_lookup() {
        SecurityConfig config = new SecurityConfig(
            mock(JwtAuthenticationFilter.class),
            mock(ApiAuthenticationEntryPoint.class),
            mock(ApiAccessDeniedHandler.class),
            mock(RequestMappingHandlerMapping.class)
        );

        assertThatThrownBy(() -> config.userDetailsService().loadUserByUsername("member"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("JWT authentication");
    }
}
