package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

class RecruitingGraphQlControllerTest {

    private static final String CONTROLLER_CLASS_NAME =
        "com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlController";

    @Test
    @DisplayName("Recruiting GraphQL controller는 Spring Controller로 등록된다")
    void Recruiting_GraphQL_controller는_Spring_Controller로_등록된다() throws ClassNotFoundException {
        Class<?> controllerType = Class.forName(CONTROLLER_CLASS_NAME);

        assertThat(controllerType)
            .hasAnnotation(Controller.class);
    }

    @Test
    @DisplayName("Recruiting GraphQL controller는 공개 모집 조회 QueryMapping을 제공한다")
    void Recruiting_GraphQL_controller는_공개_모집_조회_QueryMapping을_제공한다() throws ClassNotFoundException {
        Class<?> controllerType = Class.forName(CONTROLLER_CLASS_NAME);

        assertThat(queryMappingFields(controllerType))
            .contains("recruitingApplicationForms", "recruitingApplicationResult");
    }

    private static Set<String> queryMappingFields(Class<?> controllerType) {
        return Arrays.stream(controllerType.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class))
            .map(method -> queryMappingField(method.getName(), method.getAnnotation(QueryMapping.class)))
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String queryMappingField(String methodName, QueryMapping queryMapping) {
        if (!queryMapping.name().isBlank()) {
            return queryMapping.name();
        }
        if (!queryMapping.value().isBlank()) {
            return queryMapping.value();
        }
        return methodName;
    }
}
