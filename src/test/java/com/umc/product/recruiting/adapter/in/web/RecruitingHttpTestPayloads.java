package com.umc.product.recruiting.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class RecruitingHttpTestPayloads {

    private RecruitingHttpTestPayloads() {
    }

    static String restCreate(ObjectMapper objectMapper, String email) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new RestCreateRequest(100L, "지원자", email, "PLAN"));
    }

    static String graphQlMutation(ObjectMapper objectMapper, String email) throws JsonProcessingException {
        String document = """
            mutation Create($input: CreateRecruitingApplicationInput!) {
              createRecruitingApplication(input: $input) {
                application { id status }
                credential { email applicationKey }
              }
            }
            """;
        return objectMapper.writeValueAsString(new GraphQlRequest(
            document,
            objectMapper.valueToTree(new GraphQlVariables(
                new GraphQlCreateInput(100L, "지원자", email, "PLAN")
            ))
        ));
    }

    static String graphQlQuery(ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new GraphQlRequest(
            "query { recruitingRounds(filter: {gisuId: 1, schoolIds: [2]}) { id } }",
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

    private record GraphQlCreateInput(Long applicationFormId, String applicantName, String applicantEmail,
                                      String firstChoice) {
    }
}
