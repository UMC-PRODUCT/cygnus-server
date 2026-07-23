package com.umc.product.documentation.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

public record ErrorCodeCatalogItemResponse(
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
    ErrorCodeCatalogSourceResponse source
) {

    public static ErrorCodeCatalogItemResponse from(ErrorCodeCatalogInfo.Item info) {
        return new ErrorCodeCatalogItemResponse(
            info.sequence(),
            info.domain(),
            info.code(),
            info.name(),
            info.httpStatus(),
            info.httpStatusName(),
            info.message(),
            info.description(),
            info.clientAction(),
            info.retryable(),
            info.severity(),
            info.deprecated(),
            info.replacementCode(),
            info.owners(),
            info.tags(),
            ErrorCodeCatalogSourceResponse.from(info.source())
        );
    }
}
