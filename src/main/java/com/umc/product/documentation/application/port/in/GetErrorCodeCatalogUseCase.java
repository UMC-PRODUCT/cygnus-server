package com.umc.product.documentation.application.port.in;

import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

public interface GetErrorCodeCatalogUseCase {

    ErrorCodeCatalogInfo getErrorCodeCatalog();
}
