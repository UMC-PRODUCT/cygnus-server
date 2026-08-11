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
            boolean connectionField = environment.getFieldDefinition().getArgument("first") != null
                || environment.getFieldDefinition().getArgument("last") != null;
            if (!connectionField) {
                return childComplexity + 1;
            }
            return childComplexity + 1 + resolveConnectionSize(environment.getArguments());
        };
        return new MaxQueryComplexityInstrumentation(properties.maxComplexity(), calculator);
    }

    private static int resolveConnectionSize(Map<String, Object> arguments) {
        Object first = arguments.get("first");
        Object last = arguments.get("last");
        if (first == null && last == null) {
            return DEFAULT_SIZE;
        }
        if (!isValidConnectionSize(first) || !isValidConnectionSize(last)) {
            return MAX_SIZE;
        }
        if (first != null && last != null) {
            return Math.min(((Number)first).intValue(), ((Number)last).intValue());
        }
        return ((Number)(first == null ? last : first)).intValue();
    }

    private static boolean isValidConnectionSize(Object value) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof Number number)) {
            return false;
        }
        double numericSize = number.doubleValue();
        return Double.isFinite(numericSize)
            && numericSize >= 0
            && numericSize <= MAX_SIZE
            && numericSize == Math.rint(numericSize);
    }
}
