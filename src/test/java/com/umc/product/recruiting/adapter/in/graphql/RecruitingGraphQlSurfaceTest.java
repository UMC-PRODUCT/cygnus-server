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
        "recruitingSeasonConfiguration",
        "findRecruitingInterviewScheduleCandidates",
        "sendRecruitingInterviewGuide",
        "assignRecruitingInterview",
        "applicationNo",
        "applicantIdentityKey",
        "assignment",
        "score",
        "scores",
        "saveRecruitingApplicationEvaluation",
        "availabilityFormResponseId",
        "csv",
        "totalElements",
        "totalPages",
        "hasNext"
    );

    /**
     * Connection을 반환하는 Query 루트 필드와 스키마의 Connection 타입 이름 매핑.
     */
    private static final Map<String, String> CONNECTION_ROOT_FIELDS = Map.of(
        "publicRecruitingRounds", "RecruitingPublicRoundGroupConnection",
        "recruitingRoundGroups", "RecruitingSeasonSummaryConnection",
        "recruitingRoundEvaluators", "RecruitingRoundEvaluatorConnection",
        "recruitingRoundInterviewQuestions", "RecruitingRoundInterviewQuestionConnection",
        "recruitingApplicationInterviewQuestions", "RecruitingApplicationInterviewQuestionConnection",
        "recruitingApplicationEvaluations", "RecruitingApplicationEvaluationConnection",
        "recruitingInterviewSessions", "RecruitingInterviewSessionConnection",
        "recruitingDecisionHistories", "RecruitingDecisionHistoryConnection",
        "recruitingRoundApplications", "RecruitingApplicationReviewConnection"
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
                    "node",
                    "nodes",
                    "publicRecruitingRounds",
                    "recruitingApplicationFormStructure",
                    "recruitingApplicationByCredential",
                    "recruitingApplication",
                    "recruitingRoundGroups",
                    "recruitingRoundTitleAvailable",
                    "recruitingSeason",
                    "recruitingRoundEvaluators",
                    "recruitingRoundInterviewQuestions",
                    "recruitingApplicationInterviewQuestions",
                    "recruitingApplicationEvaluations",
                    "recruitingInterviewSchedule",
                    "recruitingInterviewSession",
                    "recruitingInterviewSessions",
                    "recruitingInterviewScheduleBoard",
                    "recruitingStatusSummary",
                    "recruitingEvaluationStatistics",
                    "recruitingDecisionHistories",
                    "recruitingRoundApplications",
                    "recruitingRoundApplication"
                );
            assertThat(fieldNames(data, "Mutation"))
                .contains(
                    "createRecruitingSeason",
                    "updateRecruitingSeason",
                    "replaceRecruitingSeasonTrackQuotas",
                    "createRecruitingRound",
                    "updateRecruitingRoundStatus",
                    "updateRecruitingRound",
                    "upsertRecruitingApplicationForm",
                    "cloneRecruitingRound",
                    "deleteRecruitingRound",
                    "addRecruitingRoundEvaluator",
                    "removeRecruitingRoundEvaluator",
                    "createRecruitingRoundInterviewQuestion",
                    "updateRecruitingRoundInterviewQuestion",
                    "deactivateRecruitingRoundInterviewQuestion",
                    "createRecruitingApplicationInterviewQuestion",
                    "updateRecruitingApplicationInterviewQuestion",
                    "deactivateRecruitingApplicationInterviewQuestion",
                    "createRecruitingApplicationDraft",
                    "createAnonymousRecruitingApplicationDraft",
                    "updateRecruitingApplicationDraft",
                    "updateAnonymousRecruitingApplication",
                    "submitRecruitingApplication",
                    "submitAnonymousRecruitingApplication",
                    "cancelRecruitingApplication",
                    "cancelAnonymousRecruitingApplication",
                    "decideRecruitingDocument",
                    "decideRecruitingFinal",
                    "prepareRecruitingRegistration",
                    "cancelRecruitingRegistration",
                    "confirmRecruitingRegistration",
                    "skipRecruitingInterview",
                    "requestRecruitingInterviewAvailability",
                    "submitRecruitingInterviewAvailability",
                    "confirmRecruitingInterviewSchedule",
                    "createRecruitingInterviewSession",
                    "updateRecruitingInterviewSession",
                    "deleteRecruitingInterviewSession",
                    "confirmRecruitingInterviewSchedules",
                    "submitRecruitingApplicationEvaluation"
                );
            for (Map<String, Object> mutationField : fields(type(data, "Mutation"))) {
                String mutationName = (String)mutationField.get("name");
                List<Map<String, Object>> args = castNullableList(mutationField.get("args"));
                assertThat(args)
                    .as("mutation %s는 단일 input 인자만 받는다", mutationName)
                    .hasSize(1);
                assertThat(args.getFirst().get("name")).isEqualTo("input");
                assertThat(renderType(castMap(args.getFirst().get("type"))))
                    .as("mutation %s input 인자는 non-null input 타입이다", mutationName)
                    .endsWith("Input!");
                String expectedPayloadType =
                    Character.toUpperCase(mutationName.charAt(0)) + mutationName.substring(1) + "Payload!";
                assertThat(renderType(castMap(mutationField.get("type"))))
                    .as("mutation %s는 전용 Payload 타입을 반환한다", mutationName)
                    .isEqualTo(expectedPayloadType);
            }
            assertThat(allFieldNames(data)).doesNotContainAnyElementsOf(REMOVED_FIELDS);
        } finally {
            Introspection.enabledJvmWide(previousIntrospectionEnabled);
        }
    }

    @Test
    @DisplayName("GraphQL introspection은 Relay Node와 Connection 계약을 제공한다")
    void GraphQL_introspection은_Relay_Node와_Connection_계약을_제공한다() throws IOException {
        // Given
        boolean previousIntrospectionEnabled = Introspection.enabledJvmWide(true);
        try {
            GraphQlSource graphQlSource = graphQlSource();

            // When
            ExecutionResult result = graphQlSource.graphQl().execute(introspectionDocument());

            // Then
            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(fieldType(data, "Query", "node")).isEqualTo("Node");
            assertThat(argType(data, "Query", "node", "id")).isEqualTo("ID!");
            assertThat(fieldType(data, "Query", "nodes")).isEqualTo("[Node]!");
            assertThat(argType(data, "Query", "nodes", "ids")).isEqualTo("[ID!]!");

            assertThat(interfaceNames(data, "RecruitingApplication")).contains("Node");
            assertThat(fieldType(data, "RecruitingApplication", "id")).isEqualTo("ID!");
            assertThat(fieldNames(data, "RecruitingApplication")).doesNotContain("applicationId");
            assertThat(interfaceNames(data, "RecruitingSeason")).contains("Node");
            assertThat(fieldType(data, "RecruitingSeason", "id")).isEqualTo("ID!");
            assertThat(fieldType(data, "Query", "recruitingSeason")).isEqualTo("RecruitingSeason!");
            assertThat(argType(data, "Query", "recruitingSeason", "id")).isEqualTo("ID!");

            assertThat(fieldType(data, "PageInfo", "hasNextPage")).isEqualTo("Boolean!");
            assertThat(fieldType(data, "PageInfo", "hasPreviousPage")).isEqualTo("Boolean!");
            assertThat(fieldType(data, "PageInfo", "startCursor")).isEqualTo("String");
            assertThat(fieldType(data, "PageInfo", "endCursor")).isEqualTo("String");

            CONNECTION_ROOT_FIELDS.forEach((rootField, connectionType) -> {
                String edgeType = connectionType.replace("Connection", "Edge");
                assertThat(fieldType(data, "Query", rootField))
                    .as("query %s는 Connection을 반환한다", rootField)
                    .isEqualTo(connectionType + "!");
                assertThat(argType(data, "Query", rootField, "first")).isEqualTo("Int");
                assertThat(argType(data, "Query", rootField, "after")).isEqualTo("String");
                assertThat(argType(data, "Query", rootField, "last")).isEqualTo("Int");
                assertThat(argType(data, "Query", rootField, "before")).isEqualTo("String");
                assertThat(fieldType(data, connectionType, "edges")).isEqualTo("[" + edgeType + "!]!");
                assertThat(fieldType(data, connectionType, "pageInfo")).isEqualTo("PageInfo!");
                assertThat(fieldType(data, connectionType, "totalCount")).isEqualTo("Long!");
                assertThat(fieldType(data, edgeType, "cursor")).isEqualTo("String!");
                assertThat(fieldType(data, edgeType, "node"))
                    .isEqualTo(edgeType.replace("Edge", "") + "!");
            });

            assertThat(fieldType(data, "RecruitingDecisionHistoryConnection", "asOf")).isEqualTo("Instant!");
            assertThat(fieldType(data, "RecruitingDecisionHistoryConnection", "progressStatus"))
                .isEqualTo("RecruitingEvaluationProgressStatus!");
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
            assertThat(fieldType(data, "Mutation", "submitRecruitingInterviewAvailability"))
                .isEqualTo("SubmitRecruitingInterviewAvailabilityPayload!");
            assertThat(fieldType(data, "SubmitRecruitingInterviewAvailabilityPayload", "success"))
                .isEqualTo("Boolean!");
            assertThat(fieldType(data, "CancelAnonymousRecruitingApplicationPayload", "application"))
                .isEqualTo("RecruitingApplication!");
            assertThat(inputFieldType(data, "SubmitRecruitingInterviewAvailabilityInput", "applicationId"))
                .isEqualTo("ID!");
            assertThat(inputFieldType(data, "SubmitRecruitingInterviewAvailabilityInput", "times"))
                .isEqualTo("[Instant!]!");
            assertThat(inputFieldType(data, "ConfirmRecruitingInterviewScheduleInput", "applicationId"))
                .isEqualTo("ID!");
            assertThat(inputFieldType(data, "ConfirmRecruitingInterviewScheduleInput", "sessionId")).isEqualTo("ID!");
            assertThat(inputFieldType(data, "CreateRecruitingInterviewSessionInput", "slotDurationMinutes"))
                .isEqualTo("Int!");
            assertThat(fieldType(data, "RecruitingInterviewSession", "slotDurationMinutes")).isEqualTo("Int!");
            assertThat(fieldType(data, "RecruitingInterviewScheduleBoard", "sessions"))
                .isEqualTo("[RecruitingInterviewScheduleBoardSession!]!");
            assertThat(inputFieldNames(data))
                .doesNotContain("memberId", "availabilityFormResponseId", "page", "size");
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
                  interfaces { name }
                  fields {
                    name
                    type { ...TypeRef }
                    args {
                      name
                      type { ...TypeRef }
                    }
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

    private static List<String> interfaceNames(Map<String, Object> data, String typeName) {
        return castNullableList(type(data, typeName).get("interfaces")).stream()
            .map(candidate -> (String)candidate.get("name"))
            .toList();
    }

    private static Map<String, Object> field(Map<String, Object> data, String typeName, String fieldName) {
        return fields(type(data, typeName)).stream()
            .filter(candidate -> fieldName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 필드를 찾을 수 없습니다: " + fieldName));
    }

    private static String fieldType(Map<String, Object> data, String typeName, String fieldName) {
        return renderType(castMap(field(data, typeName, fieldName).get("type")));
    }

    private static String argType(Map<String, Object> data, String typeName, String fieldName, String argName) {
        Map<String, Object> arg = castNullableList(field(data, typeName, fieldName).get("args")).stream()
            .filter(candidate -> argName.equals(candidate.get("name")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GraphQL 인자를 찾을 수 없습니다: " + argName));
        return renderType(castMap(arg.get("type")));
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
