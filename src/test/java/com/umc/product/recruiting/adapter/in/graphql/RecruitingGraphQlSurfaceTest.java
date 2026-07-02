package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;

import com.umc.product.global.config.GraphQlRuntimeWiringConfig;

import graphql.ExecutionResult;
import graphql.introspection.Introspection;

class RecruitingGraphQlSurfaceTest {

    @Test
    @DisplayName("GraphQL introspection에서 Recruiting Query와 Mutation 필드를 제공한다")
    void GraphQL_introspection에서_Recruiting_Query와_Mutation_필드를_제공한다() throws IOException {
        // Given
        boolean previousIntrospectionEnabled = Introspection.enabledJvmWide(true);
        try {
            GraphQlSource graphQlSource = graphQlSource();

            // When
            ExecutionResult result = graphQlSource.graphQl()
                .execute("""
                    {
                      __schema {
                        types {
                          name
                          fields {
                            name
                          }
                        }
                      }
                    }
                    """)
                ;

            // Then
            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data).isNotNull();
            List<String> queryFields = fieldNames(data, "Query");
            List<String> mutationFields = fieldNames(data, "Mutation");
            System.out.println("GraphQL introspection Query fields: " + queryFields);
            System.out.println("GraphQL introspection Mutation fields: " + mutationFields);

            assertThat(queryFields)
                .contains(
                    "recruitingApplicationForms",
                    "recruitingApplicationResult",
                    "recruitingStatusSummary",
                    "recruitingVisibleInterviewEvaluations"
                );
            assertThat(mutationFields)
                .contains(
                    "createRecruitingSeason",
                    "linkRecruitingApplicationForm",
                    "createRecruitingApplicationDraft",
                    "confirmRecruitingRegistration",
                    "submitRecruitingInterviewEvaluation"
                );
        } finally {
            Introspection.enabledJvmWide(previousIntrospectionEnabled);
        }
    }

    private static GraphQlSource graphQlSource() throws IOException {
        Resource[] schemaResources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:graphql/**/*.graphqls");

        return GraphQlSource.schemaResourceBuilder()
            .schemaResources(schemaResources)
            .configureRuntimeWiring(new GraphQlRuntimeWiringConfig().graphQlRuntimeWiringConfigurer())
            .build();
    }

    @SuppressWarnings("unchecked")
    private static List<String> fieldNames(Map<String, Object> data, String typeName) {
        Map<String, Object> schema = (Map<String, Object>) data.get("__schema");
        List<Map<String, Object>> types = (List<Map<String, Object>>) schema.get("types");
        Map<String, Object> type = types.stream()
            .filter(candidate -> typeName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 타입을 찾을 수 없습니다: " + typeName));
        List<Map<String, String>> fields = (List<Map<String, String>>) type.get("fields");
        return fields.stream()
            .map(field -> field.get("name"))
            .toList();
    }
}
