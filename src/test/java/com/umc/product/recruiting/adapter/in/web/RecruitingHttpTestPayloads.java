package com.umc.product.recruiting.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

final class RecruitingHttpTestPayloads {

    private RecruitingHttpTestPayloads() {
    }

    static String restCreate(ObjectMapper objectMapper, String email) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new RestCreateRequest(100L, "지원자", email, "PLAN"));
    }

    static String graphQlMutation(ObjectMapper objectMapper, String email) throws JsonProcessingException {
        String document = """
            mutation Create($input: CreateRecruitingApplicationDraftInput!) {
              createRecruitingApplicationDraft(input: $input) { applicationId applicationKey status }
            }
            """;
        return objectMapper.writeValueAsString(new GraphQlRequest(
            document,
            objectMapper.valueToTree(new GraphQlVariables(
                new GraphQlCreateInput(
                    GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION_FORM, 100L),
                    "지원자",
                    email,
                    "PLAN"
                )
            ))
        ));
    }

    static String graphQlQuery(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new GraphQlRequest(
            """
                query {
                  publicRecruitingRounds(input: {gisuId: "%s", schoolIds: ["%s"]}) {
                    edges { node { seasonId rounds { roundId } } }
                  }
                }
                """.formatted(
                    GlobalId.encode(GlobalIdTypes.GISU, 1L),
                    GlobalId.encode(GlobalIdTypes.SCHOOL, 2L)
                ),
            objectMapper.createObjectNode()
        ));
    }

    static String redactResponse(ObjectMapper objectMapper, String responseBody) throws JsonProcessingException {
        JsonNode body = objectMapper.readTree(responseBody);
        body.findParents("applicationKey").forEach(parent ->
            ((ObjectNode)parent).put("applicationKey", "[REDACTED]")
        );
        return objectMapper.writeValueAsString(body);
    }

    private record RestCreateRequest(Long applicationFormId, String applicantName, String applicantEmail,
                                     String firstChoice) {
    }

    private record GraphQlRequest(String query, JsonNode variables) {
    }

    private record GraphQlVariables(GraphQlCreateInput input) {
    }

    private record GraphQlCreateInput(String applicationFormId, String applicantName, String applicantEmail,
                                      String firstChoice) {
    }
}
