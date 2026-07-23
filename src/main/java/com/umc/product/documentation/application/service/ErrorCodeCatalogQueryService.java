package com.umc.product.documentation.application.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.documentation.application.port.in.GetErrorCodeCatalogUseCase;
import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;
import com.umc.product.documentation.domain.DocumentationDomainException;
import com.umc.product.documentation.domain.DocumentationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ErrorCodeCatalogQueryService implements GetErrorCodeCatalogUseCase {

    private static final String CATALOG_RESOURCE_PATH = "static/docs/catalog/error/catalog.json";

    private final ObjectMapper objectMapper;

    private volatile ErrorCodeCatalogInfo cachedCatalog;

    @Override
    public ErrorCodeCatalogInfo getErrorCodeCatalog() {
        ErrorCodeCatalogInfo snapshot = cachedCatalog;
        if (snapshot == null) {
            synchronized (this) {
                snapshot = cachedCatalog;
                if (snapshot == null) {
                    snapshot = loadCatalog();
                    cachedCatalog = snapshot;
                }
            }
        }

        return snapshot;
    }

    private ErrorCodeCatalogInfo loadCatalog() {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, ErrorCodeCatalogInfo.class);
        } catch (IOException exception) {
            throw new DocumentationDomainException(
                DocumentationErrorCode.ERROR_CODE_CATALOG_UNAVAILABLE,
                exception
            );
        }
    }
}
