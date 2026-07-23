package com.umc.product.documentation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;

@DisplayName("ErrorCodeCatalogQueryService")
class ErrorCodeCatalogQueryServiceTest {

    private final ErrorCodeCatalogQueryService service = new ErrorCodeCatalogQueryService(new ObjectMapper());

    @Test
    @DisplayName("생성된 ErrorCode v1 manifest를 classpath에서 읽는다")
    void 생성된_ErrorCode_v1_manifest를_classpath에서_읽는다() {
        ErrorCodeCatalogInfo catalog = service.getErrorCodeCatalog();

        assertThat(catalog.schemaVersion()).isEqualTo(1);
        assertThat(catalog.service()).isEqualTo("umc-product-server");
        assertThat(catalog.generatedAt()).isNull();
        assertThat(catalog.totalCount()).isEqualTo(catalog.items().size());
        assertThat(catalog.items()).isNotEmpty();
        assertThat(catalog.items()).allSatisfy(item -> {
            assertThat(item.owners()).isNotNull();
            assertThat(item.tags()).isNotNull();
            assertThat(item.source().enumName()).isNotBlank();
            assertThat(item.source().file()).isNotBlank();
            assertThat(item.source().line()).isPositive();
        });
    }

    @Test
    @DisplayName("metadata 선언이 없으면 optional field 기본값을 manifest에 반영한다")
    void metadata_선언이_없으면_optional_field_기본값을_manifest에_반영한다() {
        ErrorCodeCatalogInfo catalog = service.getErrorCodeCatalog();

        ErrorCodeCatalogInfo.Item item = catalog.items().stream()
            .filter(candidate -> "DOCS-0001".equals(candidate.code()))
            .findFirst()
            .orElseThrow();

        assertThat(item.description()).isNull();
        assertThat(item.clientAction()).isNull();
        assertThat(item.retryable()).isNull();
        assertThat(item.severity()).isNull();
        assertThat(item.deprecated()).isFalse();
        assertThat(item.replacementCode()).isNull();
        assertThat(item.owners()).isEmpty();
        assertThat(item.tags()).isEmpty();
    }

    @Test
    @DisplayName("생성된 ErrorCode manifest를 한 번만 읽고 캐싱한다")
    void 생성된_ErrorCode_manifest를_한_번만_읽고_캐싱한다() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ErrorCodeCatalogInfo response = new ErrorCodeCatalogInfo(
            1,
            "umc-product-server",
            null,
            0,
            List.of()
        );
        given(objectMapper.readValue(any(InputStream.class), eq(ErrorCodeCatalogInfo.class)))
            .willReturn(response);
        ErrorCodeCatalogQueryService cachedService = new ErrorCodeCatalogQueryService(objectMapper);

        ErrorCodeCatalogInfo first = cachedService.getErrorCodeCatalog();
        ErrorCodeCatalogInfo second = cachedService.getErrorCodeCatalog();

        assertThat(first).isSameAs(response);
        assertThat(second).isSameAs(response);
        verify(objectMapper).readValue(any(InputStream.class), eq(ErrorCodeCatalogInfo.class));
    }
}
