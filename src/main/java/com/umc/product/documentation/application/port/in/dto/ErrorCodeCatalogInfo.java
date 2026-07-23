package com.umc.product.documentation.application.port.in.dto;

import java.util.List;

public record ErrorCodeCatalogInfo(
    int schemaVersion,
    String service,
    String generatedAt,
    int totalCount,
    List<Item> items
) {

    public record Item(
        int sequence,
        String domain,
        String code,
        String name,
        int httpStatus,
        String httpStatusName,
        String message,
        String description,
        String clientAction,
        Boolean retryable,
        String severity,
        boolean deprecated,
        String replacementCode,
        List<String> owners,
        List<String> tags,
        Source source
    ) {
    }

    public record Source(
        String enumName,
        String file,
        int line
    ) {
    }
}
