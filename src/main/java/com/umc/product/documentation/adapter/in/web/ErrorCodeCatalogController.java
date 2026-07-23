package com.umc.product.documentation.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.documentation.adapter.in.web.dto.response.ErrorCodeCatalogResponse;
import com.umc.product.documentation.application.port.in.GetErrorCodeCatalogUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/docs/error-codes")
@RequiredArgsConstructor
public class ErrorCodeCatalogController {

    private final GetErrorCodeCatalogUseCase getErrorCodeCatalogUseCase;

    @GetMapping
    public ErrorCodeCatalogResponse getErrorCodeCatalog() {
        return ErrorCodeCatalogResponse.from(getErrorCodeCatalogUseCase.getErrorCodeCatalog());
    }
}
