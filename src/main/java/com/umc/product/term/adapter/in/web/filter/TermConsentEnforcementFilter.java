package com.umc.product.term.adapter.in.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.response.ApiResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.util.PublicEndpointCollector;
import com.umc.product.term.domain.exception.TermErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TermConsentEnforcementFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    private static final List<AllowedEndpoint> RECONSENT_FLOW_ENDPOINTS = List.of(
        new AllowedEndpoint("GET", "/api/v1/terms/**"),
        new AllowedEndpoint("POST", "/api/v1/terms/agreements")
    );

    private static final List<AllowedEndpoint> INFRASTRUCTURE_ENDPOINTS = List.of(
        new AllowedEndpoint(null, "/actuator/**"),
        new AllowedEndpoint(null, "/error"),
        new AllowedEndpoint(null, "/swagger-ui/**"),
        new AllowedEndpoint(null, "/swagger-ui.html"),
        new AllowedEndpoint(null, "/docs/**"),
        new AllowedEndpoint(null, "/v3/api-docs/**"),
        new AllowedEndpoint(null, "/docs-json/**"),
        new AllowedEndpoint(null, "/swagger-resources/**"),
        new AllowedEndpoint(null, "/webjars/**"),
        new AllowedEndpoint(null, "/umc-logo.svg")
    );

    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private volatile List<PublicEndpointCollector.EndpointMatcher> publicEndpoints;

    @Autowired
    public TermConsentEnforcementFilter(
        ObjectMapper objectMapper,
        RequestMappingHandlerMapping requestMappingHandlerMapping
    ) {
        this.objectMapper = objectMapper;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    TermConsentEnforcementFilter(
        ObjectMapper objectMapper,
        List<PublicEndpointCollector.EndpointMatcher> publicEndpoints
    ) {
        this.objectMapper = objectMapper;
        this.requestMappingHandlerMapping = null;
        this.publicEndpoints = List.copyOf(publicEndpoints);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (isAllowedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        MemberPrincipal memberPrincipal = resolveMemberPrincipal();
        if (memberPrincipal == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (memberPrincipal.isRequiredTermsAgreed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeReconsentRequiredResponse(response);
    }

    private boolean isAllowedEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = URL_PATH_HELPER.getPathWithinApplication(request);

        return RECONSENT_FLOW_ENDPOINTS.stream().anyMatch(endpoint -> endpoint.matches(method, uri))
            || INFRASTRUCTURE_ENDPOINTS.stream().anyMatch(endpoint -> endpoint.matches(method, uri))
            || publicEndpoints().stream().anyMatch(endpoint -> matchesPublicEndpoint(endpoint, method, uri));
    }

    private List<PublicEndpointCollector.EndpointMatcher> publicEndpoints() {
        List<PublicEndpointCollector.EndpointMatcher> endpoints = publicEndpoints;
        if (endpoints != null) {
            return endpoints;
        }

        synchronized (this) {
            if (publicEndpoints == null) {
                List<PublicEndpointCollector.EndpointMatcher> collectedEndpoints =
                    PublicEndpointCollector.collectPublicEndpoints(requestMappingHandlerMapping);
                publicEndpoints = List.copyOf(collectedEndpoints);
            }
            return publicEndpoints;
        }
    }

    private boolean matchesPublicEndpoint(
        PublicEndpointCollector.EndpointMatcher endpoint,
        String requestMethod,
        String requestUri
    ) {
        HttpMethod method = endpoint.method();
        return (method == null || method.matches(requestMethod))
            && PATH_MATCHER.match(endpoint.pattern(), requestUri);
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

    private void writeReconsentRequiredResponse(HttpServletResponse response) throws IOException {
        TermErrorCode errorCode = TermErrorCode.TERMS_RECONSENT_REQUIRED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> payload = ApiResponse.onFailure(
            errorCode.getCode(),
            errorCode.getMessage(),
            null
        );

        objectMapper.writeValue(response.getWriter(), payload);
    }

    private record AllowedEndpoint(String method, String pattern) {

        private boolean matches(String requestMethod, String requestUri) {
            return (method == null || method.equalsIgnoreCase(requestMethod))
                && PATH_MATCHER.match(pattern, requestUri);
        }
    }
}
