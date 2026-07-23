package com.umc.product.documentation.adapter.in.web.dto.response;

import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

public record ErrorCodeCatalogSourceResponse(
    String enumName,
    String file,
    int line
) {

    public static ErrorCodeCatalogSourceResponse from(ErrorCodeCatalogInfo.Source info) {
        return new ErrorCodeCatalogSourceResponse(info.enumName(), info.file(), info.line());
    }
}
