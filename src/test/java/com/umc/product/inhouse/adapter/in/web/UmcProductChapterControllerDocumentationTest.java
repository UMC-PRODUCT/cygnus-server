package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.umc.product.inhouse.adapter.in.web.dto.request.CreateUmcProductChapterRequest;
import com.umc.product.inhouse.adapter.in.web.dto.request.UpdateUmcProductChapterRequest;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;
import com.umc.product.support.DocumentationTest;

class UmcProductChapterControllerDocumentationTest extends DocumentationTest {

    @Test
    @DisplayName("UMC PRODUCT Chapter를 생성한다")
    void UMC_PRODUCT_Chapter를_생성한다() throws Exception {
        // given
        CreateUmcProductChapterRequest request = new CreateUmcProductChapterRequest(
            "DEV", "Development", "개발 Chapter", 1, true
        );

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/chapters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(requestFields(
                fieldWithPath("code").type(JsonFieldType.STRING).description("Chapter 코드"),
                fieldWithPath("name").type(JsonFieldType.STRING).description("Chapter 이름"),
                fieldWithPath("description").type(JsonFieldType.STRING).description("Chapter 설명").optional(),
                fieldWithPath("sortOrder").type(JsonFieldType.STRING).description("정렬 순서").optional(),
                fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT Chapter 목록을 조회한다")
    void UMC_PRODUCT_Chapter_목록을_조회한다() throws Exception {
        // given
        given(getUmcProductChapterUseCase.list(true)).willReturn(List.of(
            new UmcProductChapterInfo(10L, "DEV", "Development", "개발 Chapter", 1, true)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/chapters").param("active", "true"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("active").description("활성 여부 필터").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT Chapter를 수정한다")
    void UMC_PRODUCT_Chapter를_수정한다() throws Exception {
        // given
        UpdateUmcProductChapterRequest request = new UpdateUmcProductChapterRequest(
            "DESIGN", "Design", "디자인 Chapter", 2, true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/umc-product/chapters/{chapterId}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                pathParameters(parameterWithName("chapterId").description("Chapter ID")),
                requestFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("Chapter 코드").optional(),
                    fieldWithPath("name").type(JsonFieldType.STRING).description("Chapter 이름").optional(),
                    fieldWithPath("description").type(JsonFieldType.STRING).description("Chapter 설명").optional(),
                    fieldWithPath("sortOrder").type(JsonFieldType.STRING).description("정렬 순서").optional(),
                    fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부").optional()
                )
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT Chapter를 삭제한다")
    void UMC_PRODUCT_Chapter를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/chapters/{chapterId}", 10L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("chapterId").description("Chapter ID")
            )));
    }

    @Test
    @DisplayName("제거된 UMC PRODUCT Part API는 404를 반환한다")
    void 제거된_UMC_PRODUCT_Part_API는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/umc-product/parts"))
            .andExpect(status().isNotFound());
    }
}
