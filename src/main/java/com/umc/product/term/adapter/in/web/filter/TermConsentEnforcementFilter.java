package com.umc.product.term.adapter.in.web.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import com.umc.product.global.config.SecurityPathConfig;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.response.ApiErrorResponseWriter;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.config.TermConsentEnforcementProperties;
import com.umc.product.term.domain.exception.TermErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TermConsentEnforcementFilter extends OncePerRequestFilter {

    private static final String METRIC_DOMAIN = "terms";
    private static final String METRIC_OPERATION = "reconsent_enforcement";
    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<AllowedEndpoint> ALLOWED_ENDPOINTS = List.of(
        AllowedEndpoint.of(HttpMethod.GET, "/api/v1/terms"),
        AllowedEndpoint.of(HttpMethod.GET, "/api/v1/terms/**"),
        AllowedEndpoint.of(HttpMethod.POST, "/api/v1/terms/agreements"),
        AllowedEndpoint.of(HttpMethod.POST, "/api/v1/auth/token/renew"),
        AllowedEndpoint.of(HttpMethod.POST, "/api/v1/auth/logout"),
        AllowedEndpoint.of(HttpMethod.POST, "/api/v1/auth/sso/logout"),
        AllowedEndpoint.of(HttpMethod.DELETE, "/api/v1/member"),
        AllowedEndpoint.any("/actuator/health"),
        AllowedEndpoint.any("/actuator/health/**"),
        AllowedEndpoint.any("/error"),
        AllowedEndpoint.any(SecurityPathConfig.GRAPHQL_PATH),
        AllowedEndpoint.any(SecurityPathConfig.GRAPHIQL_PATH),
        AllowedEndpoint.any(SecurityPathConfig.GRAPHIQL_PATTERN),
        AllowedEndpoint.any("/ws/**")
    );

    private final TermConsentEnforcementProperties properties;
    private final OperationalMetrics operationalMetrics;
    private final ApiErrorResponseWriter errorResponseWriter;

    public TermConsentEnforcementFilter(
        TermConsentEnforcementProperties properties,
        OperationalMetrics operationalMetrics,
        ApiErrorResponseWriter errorResponseWriter
    ) {
        this.properties = properties;
        this.operationalMetrics = operationalMetrics;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        MemberPrincipal memberPrincipal = resolveMemberPrincipal();
        if (memberPrincipal == null || memberPrincipal.isRequiredTermsAgreed()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        operationalMetrics.recordSecurityEvent(METRIC_DOMAIN, METRIC_OPERATION, "blocked_rest");
        errorResponseWriter.write(response, TermErrorCode.TERMS_RECONSENT_REQUIRED);
    }

    private boolean isAllowedEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String path = URL_PATH_HELPER.getPathWithinApplication(request);

        if (SecurityPathConfig.DOCUMENTATION_PATHS.stream()
            .anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
            return true;
        }

        return ALLOWED_ENDPOINTS.stream().anyMatch(endpoint -> endpoint.matches(method, path));
    }

    private MemberPrincipal resolveMemberPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal;
        }

        return null;
    }

    private record AllowedEndpoint(HttpMethod method, String pattern) {

        private static AllowedEndpoint any(String pattern) {
            return new AllowedEndpoint(null, pattern);
        }

        private static AllowedEndpoint of(HttpMethod method, String pattern) {
            return new AllowedEndpoint(method, pattern);
        }

        private boolean matches(String requestMethod, String requestPath) {
            return (method == null || method.matches(requestMethod))
                && PATH_MATCHER.match(pattern, requestPath);
        }
    }
}
