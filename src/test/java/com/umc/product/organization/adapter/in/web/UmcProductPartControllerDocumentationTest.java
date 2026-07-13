package com.umc.product.organization.adapter.in.web;

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

import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductPartRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductPartRequest;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;
import com.umc.product.support.DocumentationTest;

class UmcProductPartControllerDocumentationTest extends DocumentationTest {

    @Test
    @DisplayName("UMC PRODUCT Part를 생성한다")
    void UMC_PRODUCT_Part를_생성한다() throws Exception {
        // given
        CreateUmcProductPartRequest request = new CreateUmcProductPartRequest(
            10L, "SERVER", "Server", "서버 Part", 1, true
        );

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/parts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(requestFields(
                fieldWithPath("chapterId").type(JsonFieldType.STRING).description("상위 Chapter ID"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("Part 코드"),
                fieldWithPath("name").type(JsonFieldType.STRING).description("Part 이름"),
                fieldWithPath("description").type(JsonFieldType.STRING).description("Part 설명").optional(),
                fieldWithPath("sortOrder").type(JsonFieldType.STRING).description("정렬 순서").optional(),
                fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT Part 목록을 조회한다")
    void UMC_PRODUCT_Part_목록을_조회한다() throws Exception {
        // given
        UmcProductChapterInfo chapter = new UmcProductChapterInfo(
            10L, "DEV", "Development", "개발 Chapter", 1, true
        );
        given(getUmcProductPartUseCase.list(10L, true)).willReturn(List.of(
            new UmcProductPartInfo(20L, 10L, chapter, "SERVER", "Server", "서버 Part", 1, true)
        ));

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/parts")
                .param("chapterId", "10")
                .param("active", "true"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(queryParameters(
                parameterWithName("chapterId").description("상위 Chapter ID 필터").optional(),
                parameterWithName("active").description("활성 여부 필터").optional()
            )));
    }

    @Test
    @DisplayName("UMC PRODUCT Part를 수정한다")
    void UMC_PRODUCT_Part를_수정한다() throws Exception {
        // given
        UpdateUmcProductPartRequest request = new UpdateUmcProductPartRequest(
            "ANDROID", "Android", "Android Part", 2, false
        );

        // when & then
        mockMvc.perform(patch("/api/v1/umc-product/parts/{partId}", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(
                pathParameters(parameterWithName("partId").description("Part ID")),
                requestFields(
                    fieldWithPath("code").type(JsonFieldType.STRING).description("Part 코드").optional(),
                    fieldWithPath("name").type(JsonFieldType.STRING).description("Part 이름").optional(),
                    fieldWithPath("description").type(JsonFieldType.STRING).description("Part 설명").optional(),
                    fieldWithPath("sortOrder").type(JsonFieldType.STRING).description("정렬 순서").optional(),
                    fieldWithPath("active").type(JsonFieldType.BOOLEAN).description("활성 여부").optional()
                )
            ));
    }

    @Test
    @DisplayName("UMC PRODUCT Part를 삭제한다")
    void UMC_PRODUCT_Part를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/parts/{partId}", 20L))
            .andExpect(status().isOk())
            .andDo(restDocsHandler.document(pathParameters(
                parameterWithName("partId").description("Part ID")
            )));
    }
}
