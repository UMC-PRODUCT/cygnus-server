package com.umc.product.global.ratelimit;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class RateLimitPolicyResolver {

    private static final String METHOD_OPTIONS = "OPTIONS";

    private final ApiRateLimitProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitPolicyResolver(ApiRateLimitProperties properties) {
        this.properties = properties;
    }

    public Optional<RateLimitPolicy> resolve(
        String method,
        String routePattern,
        String requestUri,
        boolean authenticated
    ) {
        if (!properties.isEnabled()
            || routePattern == null
            || METHOD_OPTIONS.equalsIgnoreCase(method)
            || !isIncluded(effectivePath(routePattern, requestUri))
            || isExcluded(effectivePath(routePattern, requestUri))) {
            return Optional.empty();
        }

        String normalizedMethod = normalizeMethod(method);
        return resolveRoutePolicy(normalizedMethod, routePattern, authenticated)
            .or(() -> Optional.of(defaultPolicy(authenticated)));
    }

    private Optional<RateLimitPolicy> resolveRoutePolicy(
        String method,
        String routePattern,
        boolean authenticated
    ) {
        return properties.routePolicies().stream()
            .filter(routePolicy -> matchesMethod(routePolicy, method))
            .filter(routePolicy -> matchesAny(routePolicy.pathPatterns(), routePattern))
            .findFirst()
            .map(routePolicy -> {
                ApiRateLimitProperties.Limit limit = authenticated
                    ? routePolicy.authenticated()
                    : routePolicy.anonymous();
                if (limit == null) {
                    limit = authenticated ? properties.authenticatedDefault() : properties.anonymousDefault();
                }
                return toPolicy(routePolicy.name(), limit);
            });
    }

    private RateLimitPolicy defaultPolicy(boolean authenticated) {
        if (authenticated) {
            return toPolicy("authenticated-default", properties.authenticatedDefault());
        }
        return toPolicy("anonymous-default", properties.anonymousDefault());
    }

    private RateLimitPolicy toPolicy(String name, ApiRateLimitProperties.Limit limit) {
        return new RateLimitPolicy(name, limit.requestsPerSecond(), limit.requestsPerMinute());
    }

    private boolean isIncluded(String routePattern) {
        return matchesAny(properties.includePaths(), routePattern);
    }

    private boolean isExcluded(String routePattern) {
        return matchesAny(properties.excludePaths(), routePattern);
    }

    private String effectivePath(String routePattern, String requestUri) {
        if (RateLimitRouteResolver.UNMAPPED_ROUTE_PATTERN.equals(routePattern)) {
            return requestUri == null ? routePattern : requestUri;
        }
        return routePattern;
    }

    private boolean matchesAny(Iterable<String> patterns, String path) {
        for (String pattern : patterns) {
            if (pattern != null && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMethod(ApiRateLimitProperties.RoutePolicy routePolicy, String method) {
        return routePolicy.methods().isEmpty() || routePolicy.methods().contains(method);
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "";
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }
}
