package com.umc.product.global.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.TimeoutWebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlInterceptor;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

@Configuration
public class GraphQlExecutionConfig {

    private static final String MEMBER_SEARCH_FIELD = "memberSearch";
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Bean
    public WebGraphQlInterceptor graphQlTimeoutWebGraphQlInterceptor(GraphQlExecutionProperties properties) {
        return new TimeoutWebGraphQlInterceptor(properties.timeout());
    }

    @Bean
    public Instrumentation graphQlMaxQueryDepthInstrumentation(GraphQlExecutionProperties properties) {
        return new MaxQueryDepthInstrumentation(properties.maxDepth());
    }

    @Bean
    public Instrumentation graphQlMaxQueryComplexityInstrumentation(GraphQlExecutionProperties properties) {
        FieldComplexityCalculator calculator = (environment, childComplexity) -> {
            if (!MEMBER_SEARCH_FIELD.equals(environment.getField().getName())) {
                return childComplexity + 1;
            }
            return childComplexity + 1 + resolveMemberSearchSize(environment.getArguments().get("page"));
        };
        return new MaxQueryComplexityInstrumentation(properties.maxComplexity(), calculator);
    }

    private static int resolveMemberSearchSize(Object page) {
        if (page == null) {
            return DEFAULT_SIZE;
        }
        if (!(page instanceof Map<?, ?> pageMap)) {
            return MAX_SIZE;
        }

        Object size = pageMap.get("size");
        if (!(size instanceof Number number)) {
            return MAX_SIZE;
        }

        double numericSize = number.doubleValue();
        if (!Double.isFinite(numericSize) || numericSize < 1 || numericSize > MAX_SIZE
            || numericSize != Math.rint(numericSize)) {
            return MAX_SIZE;
        }

        int resolvedSize = number.intValue();
        return resolvedSize;
    }
}
