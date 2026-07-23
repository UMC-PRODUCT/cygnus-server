package com.umc.product.documentation.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

public record ErrorCodeCatalogResponse(
    int schemaVersion,
    String service,
    String generatedAt,
    int totalCount,
    List<ErrorCodeCatalogItemResponse> items
) {

    public static ErrorCodeCatalogResponse from(ErrorCodeCatalogInfo info) {
        return new ErrorCodeCatalogResponse(
            info.schemaVersion(),
            info.service(),
            info.generatedAt(),
            info.totalCount(),
            info.items().stream().map(ErrorCodeCatalogItemResponse::from).toList()
        );
    }
}
