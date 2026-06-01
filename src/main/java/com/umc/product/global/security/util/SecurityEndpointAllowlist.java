package com.umc.product.global.security.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.http.HttpMethod;

public final class SecurityEndpointAllowlist {

    private static final List<SecurityEndpoint> HEALTH_ENDPOINTS = List.of(
        SecurityEndpoint.any("/actuator/**"),
        SecurityEndpoint.any("/error")
    );

    private static final List<SecurityEndpoint> STATIC_FILE_ENDPOINTS = List.of(
        SecurityEndpoint.any("/swagger-ui/**"),
        SecurityEndpoint.any("/swagger-ui.html"),
        SecurityEndpoint.any("/docs/**"),
        SecurityEndpoint.any("/v3/api-docs/**"),
        SecurityEndpoint.any("/docs-json/**"),
        SecurityEndpoint.any("/swagger-resources/**"),
        SecurityEndpoint.any("/webjars/**"),
        SecurityEndpoint.any("/umc-logo.svg")
    );

    private static final List<SecurityEndpoint> RECONSENT_FLOW_ENDPOINTS = List.of(
        SecurityEndpoint.of(HttpMethod.GET, "/api/v1/terms/**"),
        SecurityEndpoint.of(HttpMethod.POST, "/api/v1/terms/agreements")
    );

    public static final List<SecurityEndpoint> INFRASTRUCTURE_ENDPOINTS = Stream.concat(
        HEALTH_ENDPOINTS.stream(),
        STATIC_FILE_ENDPOINTS.stream()
    ).toList();

    private SecurityEndpointAllowlist() {
    }

    public static String[] staticFilePaths() {
        return STATIC_FILE_ENDPOINTS.stream()
            .map(SecurityEndpoint::pattern)
            .toArray(String[]::new);
    }

    public static List<SecurityEndpoint> permitAllEndpoints(List<SecurityEndpoint> publicEndpoints) {
        return merge(INFRASTRUCTURE_ENDPOINTS, publicEndpoints);
    }

    public static List<SecurityEndpoint> reconsentBypassEndpoints(List<SecurityEndpoint> publicEndpoints) {
        return merge(RECONSENT_FLOW_ENDPOINTS, INFRASTRUCTURE_ENDPOINTS, publicEndpoints);
    }

    @SafeVarargs
    private static List<SecurityEndpoint> merge(List<SecurityEndpoint>... endpointGroups) {
        List<SecurityEndpoint> endpoints = new ArrayList<>();
        for (List<SecurityEndpoint> endpointGroup : endpointGroups) {
            endpoints.addAll(endpointGroup);
        }
        return List.copyOf(endpoints);
    }
}
