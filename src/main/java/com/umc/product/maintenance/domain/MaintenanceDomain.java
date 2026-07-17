package com.umc.product.maintenance.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.util.AntPathMatcher;

public enum MaintenanceDomain {

    CHALLENGER(List.of(
        "/api/v1/challenger/**",
        "/api/v1/challenger-record/**"
    )),
    PROJECT(List.of(
        "/api/v1/projects/**",
        "/api/v1/project/**"
    )),
    SCHEDULE(List.of(
        "/api/v1/schedules/**",
        "/api/v1/study-groups/schedules/**"
    )),
    NOTICE(List.of(
        "/api/v1/notices/**"
    )),
    COMMUNITY(List.of(
        "/api/v1/posts/**"
    )),
    ORGANIZATION(List.of(
        "/api/v1/gisu/**",
        "/api/v1/schools/**",
        "/api/v1/chapters/**",
        "/api/v1/study-groups/**"
    )),
    NOTIFICATION(List.of(
        "/api/v1/notification/**"
    )),
    MEMBER(List.of(
        "/api/v1/member/**"
    )),
    STORAGE(List.of(
        "/api/v1/storage/**"
    )),
    AUTHORIZATION(List.of(
        "/api/v1/authorization/**"
    )),
    ;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final List<String> uriPatterns;

    MaintenanceDomain(List<String> uriPatterns) {
        this.uriPatterns = uriPatterns;
    }

    public boolean matches(String requestUri) {
        return uriPatterns.stream().anyMatch(p -> PATH_MATCHER.match(p, requestUri));
    }

    public static Optional<MaintenanceDomain> fromUri(String requestUri) {
        return Arrays.stream(values())
            .filter(d -> d.matches(requestUri))
            .findFirst();
    }
}
