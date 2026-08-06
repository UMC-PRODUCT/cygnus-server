package com.umc.product.organization.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.support.DocumentationTest;

class UmcProductOrganizationChartQueryControllerDocumentationTest extends DocumentationTest {

    @Test
    @DisplayName("현재 UMC PRODUCT Chapter와 Squad를 조회한다")
    void 현재_UMC_PRODUCT_조직도를_조회한다() throws Exception {
        // given
        UmcProductChapterInfo chapter = new UmcProductChapterInfo(
            10L, "DEV", "Development", "개발 Chapter", 1, true
        );
        UmcProductSquadInfo squad = new UmcProductSquadInfo(
            70L,
            "SPRINT",
            "Sprint Squad",
            "제품 개선 Squad",
            LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 12, 31),
            1,
            true
        );
        given(getUmcProductOrganizationChartUseCase.getCurrent()).willReturn(
            new UmcProductOrganizationChartInfo(
                List.of(chapter),
                List.of(squad)
            )
        );

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/organization-chart"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler);
    }
}
