package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.umc.product.organization.adapter.in.web.dto.request.CreateUmcProductChapterRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateUmcProductChapterRequest;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductChapterCommand;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.support.ControllerTestSupport;

class UmcProductChapterControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("UMC PRODUCT Chapter를 생성한다")
    void UMC_PRODUCT_Chapter를_생성한다() throws Exception {
        // given
        CreateUmcProductChapterRequest request = new CreateUmcProductChapterRequest(
            "DEV", "Development", "개발 Chapter", 1, true
        );
        given(manageUmcProductChapterUseCase.create(any())).willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/v1/umc-product/chapters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result").value("10"));

        verify(manageUmcProductChapterUseCase).create(CreateUmcProductChapterCommand.of(
            TEST_MEMBER_ID, "DEV", "Development", "개발 Chapter", 1, true
        ));
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
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.chapters.length()").value(1))
            .andExpect(jsonPath("$.result.chapters[0].chapterId").value("10"))
            .andExpect(jsonPath("$.result.chapters[0].code").value("DEV"))
            .andExpect(jsonPath("$.result.chapters[0].name").value("Development"))
            .andExpect(jsonPath("$.result.chapters[0].description").value("개발 Chapter"))
            .andExpect(jsonPath("$.result.chapters[0].sortOrder").value("1"))
            .andExpect(jsonPath("$.result.chapters[0].active").value(true));

        verify(getUmcProductChapterUseCase).list(true);
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
            .andExpect(status().isOk());

        verify(manageUmcProductChapterUseCase).update(UpdateUmcProductChapterCommand.of(
            10L, TEST_MEMBER_ID, "DESIGN", "Design", "디자인 Chapter", 2, true
        ));
    }

    @Test
    @DisplayName("UMC PRODUCT Chapter를 삭제한다")
    void UMC_PRODUCT_Chapter를_삭제한다() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/umc-product/chapters/{chapterId}", 10L))
            .andExpect(status().isOk());

        verify(manageUmcProductChapterUseCase).delete(10L, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("제거된 UMC PRODUCT Part API는 404를 반환한다")
    void 제거된_UMC_PRODUCT_Part_API는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/umc-product/parts"))
            .andExpect(status().isNotFound());
    }
}
