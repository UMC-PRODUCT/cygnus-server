package com.umc.product.organization.adapter.in.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolLinkInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.support.ControllerTestSupport;

class SchoolLinkQueryControllerTest extends ControllerTestSupport {

    @Test
    void 학교_링크를_조회합니다() throws Exception {
        // given
        Long schoolId = 1L;
        SchoolLinkInfo schoolLinkInfo = new SchoolLinkInfo(
            List.of(
                new SchoolLinkInfo.SchoolLinkItem("UMC 카카오톡", SchoolLinkType.KAKAO, "https://pf.kakao.com/_example"),
                new SchoolLinkInfo.SchoolLinkItem("UMC 인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/umc"),
                new SchoolLinkInfo.SchoolLinkItem("UMC 유튜브", SchoolLinkType.YOUTUBE, "https://youtube.com/@umc")
            )
        );

        given(getSchoolUseCase.getSchoolLink(schoolId)).willReturn(schoolLinkInfo);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/schools/link/{schoolId}", schoolId));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.links").isArray())
            .andExpect(jsonPath("$.result.links[*].title").value(contains("UMC 카카오톡", "UMC 인스타그램", "UMC 유튜브")))
            .andExpect(jsonPath("$.result.links[*].type").value(contains("KAKAO", "INSTAGRAM", "YOUTUBE")))
            .andExpect(jsonPath("$.result.links[*].url").value(contains(
                "https://pf.kakao.com/_example", "https://instagram.com/umc", "https://youtube.com/@umc")));
    }
}
