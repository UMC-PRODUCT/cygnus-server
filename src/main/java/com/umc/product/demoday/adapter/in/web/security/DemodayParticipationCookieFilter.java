package com.umc.product.demoday.adapter.in.web.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게스트 participant Cookie를 읽어 {@link DemodayParticipationPrincipal}로 SecurityContext를 채운다.
 * <p>
 * {@code JwtAuthenticationFilter}와 대칭 구조다 — 검증 실패는 예외로 요청을 끊지 않고 조용히 통과시킨다
 * (fail-open). "회원 Bearer 또는 게스트 Cookie 둘 중 하나면 통과"(이슈 #1258)를 만족하려면 두 필터 중
 * 어느 쪽이 실패해도 다른 쪽이 인증을 채울 여지가 남아야 하기 때문이다. 최종 인가는 항상
 * {@code SecurityConfig}의 {@code anyRequest().authenticated()}가 담당한다.
 * <p>
 * {@code @Component}가 아니라 {@code SecurityConfig}의 명시 {@code @Bean}으로 등록한다. {@code @WebMvcTest}
 * 슬라이스 테스트는 {@code Filter} 구현체를 자동으로 끌어오는데, 이 클래스가 컴포넌트 스캔 대상이면
 * 그 의존성({@link DemodayParticipantTokenProvider})까지 끌려 들어와 컨텍스트 로딩이 실패한다
 * (같은 이유로 분리된 {@code MaintenanceFilter} 선례 참고).
 */
@Slf4j
@RequiredArgsConstructor
public class DemodayParticipationCookieFilter extends OncePerRequestFilter {

    private final DemodayParticipantTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        /**
         * 앞선 Bearer 인증 필터가 이미 이 요청의 Authentication을 할당해 뒀는지를 검증한다.
         * 만약, null이 아닌 할당된 값이 넘어온다면 이미 인증된 member이므로 guest 인증을 스킵한다.
         */
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveCookieValue(request)
                .flatMap(tokenProvider::parseEntryCodeId)
                .ifPresent(this::authenticateAsGuest);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateAsGuest(Long entryCodeId) {
        DemodayParticipationPrincipal principal = new DemodayParticipationPrincipal(entryCodeId);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> resolveCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> DemodayParticipantTokenProvider.COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}
