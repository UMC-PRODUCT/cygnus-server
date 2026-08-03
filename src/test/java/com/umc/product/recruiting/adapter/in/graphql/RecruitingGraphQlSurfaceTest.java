package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.graphql.execution.GraphQlSource;

import com.umc.product.global.config.GraphQlRuntimeWiringConfig;

import graphql.ExecutionResult;
import graphql.introspection.Introspection;

class RecruitingGraphQlSurfaceTest {

    private static final Set<String> REMOVED_FIELDS = Set.of(
        "recruitingApplicationResult",
        "findRecruitingInterviewScheduleCandidates",
        "sendRecruitingInterviewGuide",
        "assignRecruitingInterview",
        "applicationNo",
        "applicantIdentityKey",
        "assignment",
        "assignments",
        "score",
        "scores",
        "saveRecruitingApplicationEvaluation",
        "availabilityFormResponseId",
        "csv"
    );

    @Test
    @DisplayName("GraphQL introspection은 Recruiting Query와 Mutation 계약만 제공한다")
    void GraphQL_introspection은_Recruiting_Query와_Mutation_계약만_제공한다() throws IOException {
        // Given
        boolean previousIntrospectionEnabled = Introspection.enabledJvmWide(true);
        try {
            GraphQlSource graphQlSource = graphQlSource();

            // When
            ExecutionResult result = graphQlSource.graphQl().execute(introspectionDocument());

            // Then
            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(fieldNames(data, "Query"))
                .contains(
                    "publicRecruitingRounds",
                    "recruitingApplication",
                    "recruitingRoundGroups",
                    "recruitingRoundTitleAvailable",
                    "recruitingSeasonConfiguration",
                    "recruitingRoundEvaluators",
                    "recruitingRoundInterviewQuestions",
                    "recruitingApplicationInterviewQuestions",
                    "recruitingApplicationEvaluations",
                    "recruitingInterviewSchedule",
                    "recruitingInterviewSession",
                    "recruitingInterviewSessions",
                    "recruitingInterviewScheduleBoard",
                    "recruitingStatusSummary",
                    "recruitingEvaluationStatistics"
                    , "recruitingRoundApplications"
                    , "recruitingRoundApplication"
                );
            assertThat(fieldNames(data, "Mutation"))
                .contains(
                    "replaceRecruitingSeasonTrackQuotas",
                    "updateRecruitingRound",
                    "upsertRecruitingApplicationForm",
                    "cloneRecruitingRound",
                    "deleteRecruitingRound",
                    "addRecruitingRoundEvaluator",
                    "removeRecruitingRoundEvaluator",
                    "createRecruitingRoundInterviewQuestion",
                    "createRecruitingApplicationInterviewQuestion",
                    "decideRecruitingFinal",
                    "prepareRecruitingRegistration",
                    "cancelRecruitingRegistration",
                    "confirmRecruitingRegistration",
                    "requestRecruitingInterviewAvailability",
                    "submitRecruitingInterviewAvailability",
                    "confirmRecruitingInterviewSchedule",
                    "createRecruitingInterviewSession",
                    "updateRecruitingInterviewSession",
                    "deleteRecruitingInterviewSession",
                    "confirmRecruitingInterviewSchedules",
                    "submitRecruitingApplicationEvaluation"
                );
            assertThat(allFieldNames(data)).doesNotContainAnyElementsOf(REMOVED_FIELDS);
        } finally {
            Introspection.enabledJvmWide(previousIntrospectionEnabled);
        }
    }

    @Test
    @DisplayName("GraphQL introspection은 Instant와 트랙 및 상태 nullability를 보존한다")
    void GraphQL_introspection은_Instant와_트랙_및_상태_nullability를_보존한다() throws IOException {
        // Given
        boolean previousIntrospectionEnabled = Introspection.enabledJvmWide(true);
        try {
            GraphQlSource graphQlSource = graphQlSource();

            // When
            ExecutionResult result = graphQlSource.graphQl().execute(introspectionDocument());

            // Then
            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(typeKind(data, "Instant")).isEqualTo("SCALAR");
            assertThat(fieldType(data, "RecruitingRoundConfiguration", "recruitableTracks"))
                .isEqualTo("[ChallengerTrack!]!");
            assertThat(fieldType(data, "RecruitingRoundConfiguration", "documentStartAt"))
                .isEqualTo("Instant!");
            assertThat(fieldType(data, "RecruitingApplication", "status"))
                .isEqualTo("RecruitingApplicationStatus!");
            assertThat(fieldType(data, "RecruitingApplication", "registrationStatus"))
                .isEqualTo("RecruitingApplicationRegistrationStatus!");
            assertThat(fieldType(data, "RecruitingApplication", "acceptedTrack"))
                .isEqualTo("ChallengerTrack");
            assertThat(fieldType(data, "Mutation", "submitRecruitingInterviewAvailability")).isEqualTo("Boolean!");
            assertThat(inputFieldType(data, "SubmitRecruitingInterviewAvailabilityInput", "times")).isEqualTo("[Instant!]!");
            assertThat(inputFieldType(data, "ConfirmRecruitingInterviewScheduleInput", "sessionId")).isEqualTo("ID!");
            assertThat(inputFieldType(data, "RecruitingInterviewSessionInput", "slotDurationMinutes"))
                .isEqualTo("Int!");
            assertThat(fieldType(data, "RecruitingInterviewSession", "slotDurationMinutes")).isEqualTo("Int!");
            assertThat(fieldType(data, "RecruitingInterviewScheduleBoard", "sessions"))
                .isEqualTo("[RecruitingInterviewScheduleBoardSession!]!");
            assertThat(inputFieldNames(data)).doesNotContain("memberId", "availabilityFormResponseId");
            assertThat(inputFieldNames(data, "RecruitingDecisionHistorySearchInput"))
                .contains("chapterIds", "schoolIds")
                .doesNotContain("chapterId", "schoolId");
            assertThat(inputFieldType(data, "RecruitingDecisionHistorySearchInput", "chapterIds"))
                .isEqualTo("[ID!]");
            assertThat(inputFieldType(data, "RecruitingDecisionHistorySearchInput", "schoolIds"))
                .isEqualTo("[ID!]");
            assertThat(enumValues(data, "ChallengerTrack"))
                .containsExactlyInAnyOrder(
                    "PLAN",
                    "DESIGN",
                    "WEB_PRODUCT_ENGINEER",
                    "MOBILE_PRODUCT_ENGINEER",
                    "INFRA_PLUS"
                );
            assertThat(enumValues(data, "RecruitingApplicationEvaluationDecision"))
                .containsExactlyInAnyOrder("APPROVED", "REJECTED");
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

    private static String introspectionDocument() {
        return """
            {
              __schema {
                types {
                  kind
                  name
                  fields {
                    name
                    type { ...TypeRef }
                  }
                  inputFields {
                    name
                    type { ...TypeRef }
                  }
                  enumValues { name }
                }
              }
            }
            fragment TypeRef on __Type {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType { kind name }
                }
              }
            }
            """;
    }

    private static Set<String> allFieldNames(Map<String, Object> data) {
        return types(data).stream()
            .flatMap(type -> fields(type).stream())
            .map(field -> (String)field.get("name"))
            .collect(Collectors.toSet());
    }

    private static Set<String> inputFieldNames(Map<String, Object> data) {
        return types(data).stream()
            .flatMap(type -> inputFields(type).stream())
            .map(field -> (String)field.get("name"))
            .collect(Collectors.toSet());
    }

    private static Set<String> inputFieldNames(Map<String, Object> data, String typeName) {
        return inputFields(type(data, typeName)).stream()
            .map(field -> (String)field.get("name"))
            .collect(Collectors.toSet());
    }

    private static List<String> fieldNames(Map<String, Object> data, String typeName) {
        return fields(type(data, typeName)).stream()
            .map(field -> (String)field.get("name"))
            .toList();
    }

    private static String fieldType(Map<String, Object> data, String typeName, String fieldName) {
        Map<String, Object> field = fields(type(data, typeName)).stream()
            .filter(candidate -> fieldName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 필드를 찾을 수 없습니다: " + fieldName));
        return renderType(castMap(field.get("type")));
    }

    private static String inputFieldType(Map<String, Object> data, String typeName, String fieldName) {
        Map<String, Object> field = inputFields(type(data, typeName)).stream()
            .filter(candidate -> fieldName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 입력 필드를 찾을 수 없습니다: " + fieldName));
        return renderType(castMap(field.get("type")));
    }

    private static String typeKind(Map<String, Object> data, String typeName) {
        return (String)type(data, typeName).get("kind");
    }

    private static List<String> enumValues(Map<String, Object> data, String typeName) {
        return castList(type(data, typeName).get("enumValues")).stream()
            .map(value -> (String)value.get("name"))
            .toList();
    }

    private static String renderType(Map<String, Object> type) {
        String kind = (String)type.get("kind");
        if ("NON_NULL".equals(kind)) {
            return renderType(castMap(type.get("ofType"))) + "!";
        }
        if ("LIST".equals(kind)) {
            return "[" + renderType(castMap(type.get("ofType"))) + "]";
        }
        return (String)type.get("name");
    }

    private static Map<String, Object> type(Map<String, Object> data, String typeName) {
        return types(data).stream()
            .filter(candidate -> typeName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 타입을 찾을 수 없습니다: " + typeName));
    }

    private static List<Map<String, Object>> types(Map<String, Object> data) {
        return castList(castMap(data.get("__schema")).get("types"));
    }

    private static List<Map<String, Object>> fields(Map<String, Object> type) {
        return castNullableList(type.get("fields"));
    }

    private static List<Map<String, Object>> inputFields(Map<String, Object> type) {
        return castNullableList(type.get("inputFields"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>)value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>)value;
    }

    private static List<Map<String, Object>> castNullableList(Object value) {
        return value == null ? List.of() : castList(value);
    }
}
