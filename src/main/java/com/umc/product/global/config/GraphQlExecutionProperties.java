package com.umc.product.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.graphql")
public record GraphQlExecutionProperties(
    Duration timeout,
    Integer maxDepth,
    Integer maxComplexity
) {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final int DEFAULT_MAX_DEPTH = 10;
    private static final int DEFAULT_MAX_COMPLEXITY = 200;

    public GraphQlExecutionProperties {
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (maxDepth == null) {
            maxDepth = DEFAULT_MAX_DEPTH;
        }
        if (maxComplexity == null) {
            maxComplexity = DEFAULT_MAX_COMPLEXITY;
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("GraphQL timeout must be positive");
        }
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("GraphQL maxDepth must be positive");
        }
        if (maxComplexity <= 0) {
            throw new IllegalArgumentException("GraphQL maxComplexity must be positive");
        }
    }
}
