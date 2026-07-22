package com.umc.product.recruiting.adapter.in.graphql.dto;

public record RecruitingApplicationCreatedGraphQlResponse(
    RecruitingApplicationGraphQlResponse application,
    Credential credential
) {

    public record Credential(String email, String applicationKey) {
    }
}
