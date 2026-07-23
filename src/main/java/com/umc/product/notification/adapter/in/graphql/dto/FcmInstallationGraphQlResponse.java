package com.umc.product.notification.adapter.in.graphql.dto;

public final class FcmInstallationGraphQlResponse {

    private FcmInstallationGraphQlResponse() {
    }

    public record Installation(String installationId) {
    }

    public record Deleted(String deletedInstallationId) {
    }
}
