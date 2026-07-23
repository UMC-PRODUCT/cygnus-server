package com.umc.product.documentation.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.documentation.application.port.in.GetErrorCodeCatalogUseCase;
import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DocumentationGraphQlController {

    private final GetErrorCodeCatalogUseCase getErrorCodeCatalogUseCase;

    @QueryMapping
    public ErrorCodeCatalogInfo errorCodeCatalog() {
        return getErrorCodeCatalogUseCase.getErrorCodeCatalog();
    }
}
