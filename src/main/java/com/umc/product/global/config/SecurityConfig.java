package com.umc.product.global.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.umc.product.global.response.ApiErrorResponseWriter;
import com.umc.product.global.security.ApiAccessDeniedHandler;
import com.umc.product.global.security.ApiAuthenticationEntryPoint;
import com.umc.product.global.security.JwtAuthenticationFilter;
import com.umc.product.global.security.util.PublicEndpointCollector;
import com.umc.product.maintenance.adapter.in.web.filter.MaintenanceFilter;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;
import com.umc.product.maintenance.application.service.MaintenanceStateHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize, @PostAuthorize 활성화
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    // application.yml에서 cors.allowed-origin-patterns 값을 List 형태로 주입받음
    @Value("${app.cors.allowed-origin-patterns}")
    private List<String> allowedOriginPatterns;

    /**
     * 점검 모드 필터. JWT 다음에 동작해서 점검 중 일반 사용자 요청을 503 으로 차단한다. {@code @Component} 가 아닌 명시 {@code @Bean} 으로 두는 이유: 슬라이스 테스트
     * ({@code @WebMvcTest}) 의 자동 Filter 디스커버리가 본 필터의 의존성까지 끌어와 컨텍스트 로딩을 실패시키는 것을 막기 위함이다. SecurityConfig 는 슬라이스 테스트에
     * 포함되지 않으므로 본 빈도 함께 제외된다.
     */
    @Bean
    public MaintenanceFilter maintenanceFilter(
        MaintenanceStateHolder stateHolder,
        MaintenanceBypassPolicy bypassPolicy,
        ApiErrorResponseWriter errorResponseWriter
    ) {
        return new MaintenanceFilter(stateHolder, bypassPolicy, errorResponseWriter);
    }

    /**
     * 메인 Security 체인. JWT → MaintenanceFilter → 인가 순서로 동작한다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http, MaintenanceFilter maintenanceFilter) throws Exception {
        List<PublicEndpointCollector.EndpointMatcher> publicEndpoints = PublicEndpointCollector
            .collectPublicEndpoints(requestMappingHandlerMapping);

        // ✅ 디버깅 로그
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔓 Public Endpoints 수집 결과:");
        if (publicEndpoints.isEmpty()) {
            System.out.println("  ⚠️  수집된 엔드포인트가 없습니다!");
        } else {
            publicEndpoints.forEach(endpoint -> {
                String method = endpoint.method() != null ? endpoint.method().name() : "ALL";
                System.out.println("  ✅ " + method + " " + endpoint.pattern());
            });
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)   // 폼 로그인 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)   // HTTP Basic 비활성
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Swagger UI와 기존 Swagger 문서 경로는 인증 여부와 무관하게 차단
                auth.requestMatchers(SecurityPathConfig.swaggerBlockedPaths()).denyAll();

                // 공통 공개 경로
                auth.requestMatchers(SecurityPathConfig.securityPermitAllPaths()).permitAll();

                // @Public 어노테이션이 달린 엔드포인트 (HTTP 메서드 포함)
                for (PublicEndpointCollector.EndpointMatcher endpoint : publicEndpoints) {
                    if (endpoint.method() != null) {
                        auth.requestMatchers(endpoint.method(), endpoint.pattern()).permitAll();
                    } else {
                        auth.requestMatchers(endpoint.pattern()).permitAll();
                    }
                }

                // 나머지는 인증 필요
                auth.anyRequest().authenticated();
            })
            // Spring 기본 로그인 필터 동작 전에 JWT 동작
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // JWT 로 SecurityContext 가 채워진 뒤 점검 필터에서 bypass 판정
            .addFilterAfter(maintenanceFilter, JwtAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint) // 인증 실패 시
                .accessDeniedHandler(accessDeniedHandler)           // 인가 실패 시
            );

        return http.build();
    }

    /**
     * UserDetailsService Bean을 제공하여 Spring Security의 기본 사용자 자동 생성 방지
     * <p>
     * Spring Security가 "인증 체계가 구성되어 있다"고 인식하도록 하기 위한 더미 Bean
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class) // Dummy Bean이라서 Conditional로 변경
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                "This application uses JWT authentication, not UserDetailsService");
        };
    }

    /**
     * 비밀번호 해시는 password_hash 단일 컬럼에 "{id}encoded" prefix 형태로 저장한다.
     * <p>
     * 신규 저장은 Argon2 를 기본으로 하고, 기존/외부 호환을 위해 bcrypt 검증도 함께 등록한다. 알고리즘/파라미터가 갱신되더라도 기존 해시를 그대로 검증할 수 있으며, 로그인 성공 시
     * {@link PasswordEncoder#upgradeEncoding} 으로 점진적 rehash 를 수행한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        final String defaultEncodingId = "argon2";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(defaultEncodingId, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new BCryptPasswordEncoder());

        return new DelegatingPasswordEncoder(defaultEncodingId, encoders);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        log.info("Allowed Origin Patterns for CORS: {}", allowedOriginPatterns);

        // Swagger CORS 설정
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);

        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // Trace ID 헤더를 클라이언트에서 읽을 수 있도록 노출
        configuration.setExposedHeaders(List.of("X-Trace-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
