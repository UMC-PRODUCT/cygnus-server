package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;

class GraphQlRuntimeWiringConfigTest {

    @Test
    @DisplayName("Project와 Recruiting GraphQL schema는 Long scalar wiring과 함께 로드된다")
    void project와_Recruiting_GraphQL_schema는_Long_scalar_wiring과_함께_로드된다() throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        assertThat(Arrays.stream(schemaResources).map(Resource::getFilename))
            .contains("organization.graphqls", "project.graphqls", "recruiting.graphqls");

        GraphQlSource graphQlSource = GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();

        assertThat(graphQlSource.schema().getType("Project")).isNotNull();
        assertThat(graphQlSource.schema().getType("RecruitingApplicationForm")).isNotNull();
        assertThat(graphQlSource.schema().getType("Long")).isNotNull();
    }
}
