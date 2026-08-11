package com.umc.product.inhouse.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;
import com.umc.product.support.DocumentationTest;

class UmcProductOrganizationChartQueryControllerDocumentationTest extends DocumentationTest {

    @Test
    @DisplayName("현재 UMC PRODUCT Chapter와 Department를 조회한다")
    void 현재_UMC_PRODUCT_조직도를_조회한다() throws Exception {
        // given
        UmcProductChapterInfo chapter = new UmcProductChapterInfo(
            10L, "DEV", "Development", "개발 Chapter", 1, true
        );
        UmcProductDepartmentInfo department = new UmcProductDepartmentInfo(
            70L,
            "SPRINT",
            "Sprint Department",
            "제품 개선 Department",
            null,
            LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 12, 31),
            1,
            true
        );
        given(getUmcProductOrganizationChartUseCase.getCurrent()).willReturn(
            new UmcProductOrganizationChartInfo(
                List.of(chapter),
                List.of(department)
            )
        );

        // when & then
        mockMvc.perform(get("/api/v1/umc-product/organization-chart"))
            .andExpect(status().isOk())
            .andDo(restDocsHandler);
    }
}
