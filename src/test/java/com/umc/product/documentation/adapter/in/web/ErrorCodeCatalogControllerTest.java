package com.umc.product.documentation.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.documentation.application.port.in.GetErrorCodeCatalogUseCase;
import com.umc.product.documentation.application.port.in.dto.ErrorCodeCatalogInfo;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = ErrorCodeCatalogController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ErrorCodeCatalogController")
class ErrorCodeCatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetErrorCodeCatalogUseCase getErrorCodeCatalogUseCase;

    @Test
    @DisplayName("GET /api/v1/docs/error-codes 에러 코드 목록을 ApiResponse로 반환한다")
    void 에러_코드_목록을_ApiResponse로_반환한다() throws Exception {
        given(getErrorCodeCatalogUseCase.getErrorCodeCatalog()).willReturn(new ErrorCodeCatalogInfo(
            1,
            "umc-product-server",
            null,
            1,
            List.of(new ErrorCodeCatalogInfo.Item(
                1,
                "documentation",
                "DOCS-0001",
                "ERROR_CODE_CATALOG_UNAVAILABLE",
                500,
                "INTERNAL_SERVER_ERROR",
                "에러 코드 목록을 불러오지 못했어요. 잠시 후 다시 시도해주세요.",
                null,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                List.of(),
                new ErrorCodeCatalogInfo.Source(
                    "DocumentationErrorCode",
                    "src/main/java/com/umc/product/documentation/domain/DocumentationErrorCode.java",
                    23
                )
            ))
        ));

        mockMvc.perform(get("/api/v1/docs/error-codes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.schemaVersion").value(1))
            .andExpect(jsonPath("$.result.service").value("umc-product-server"))
            .andExpect(jsonPath("$.result.generatedAt").doesNotExist())
            .andExpect(jsonPath("$.result.totalCount").value(1))
            .andExpect(jsonPath("$.result.items[0].code").value("DOCS-0001"))
            .andExpect(jsonPath("$.result.items[0].httpStatus").value(500))
            .andExpect(jsonPath("$.result.items[0].description").doesNotExist())
            .andExpect(jsonPath("$.result.items[0].clientAction").doesNotExist())
            .andExpect(jsonPath("$.result.items[0].retryable").doesNotExist())
            .andExpect(jsonPath("$.result.items[0].severity").doesNotExist())
            .andExpect(jsonPath("$.result.items[0].deprecated").value(false))
            .andExpect(jsonPath("$.result.items[0].replacementCode").doesNotExist())
            .andExpect(jsonPath("$.result.items[0].owners").isEmpty())
            .andExpect(jsonPath("$.result.items[0].tags").isEmpty())
            .andExpect(jsonPath("$.result.items[0].source.enumName").value("DocumentationErrorCode"));
    }
}
