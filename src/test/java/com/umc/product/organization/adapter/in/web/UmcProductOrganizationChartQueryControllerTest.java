package com.umc.product.organization.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.support.ControllerTestSupport;

class UmcProductOrganizationChartQueryControllerTest extends ControllerTestSupport {

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
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.chapters.length()").value(1))
            .andExpect(jsonPath("$.result.chapters[0].chapterId").value("10"))
            .andExpect(jsonPath("$.result.chapters[0].code").value("DEV"))
            .andExpect(jsonPath("$.result.chapters[0].name").value("Development"))
            .andExpect(jsonPath("$.result.chapters[0].active").value(true))
            .andExpect(jsonPath("$.result.squads.length()").value(1))
            .andExpect(jsonPath("$.result.squads[0].squadId").value("70"))
            .andExpect(jsonPath("$.result.squads[0].name").value("Sprint Squad"))
            .andExpect(jsonPath("$.result.squads[0].startDate").value("2026-07-13"))
            .andExpect(jsonPath("$.result.squads[0].endDate").value("2026-12-31"))
            .andExpect(jsonPath("$.result.squads[0].sortOrder").value("1"))
            .andExpect(jsonPath("$.result.squads[0].active").value(true));

        verify(getUmcProductOrganizationChartUseCase).getCurrent();
    }
}
