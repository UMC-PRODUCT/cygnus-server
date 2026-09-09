package com.umc.product.organization.adapter.in.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.support.ControllerTestSupport;

class ChapterQueryControllerTest extends ControllerTestSupport {

    @Test
    void 지부_목록을_조회합니다() throws Exception {
        // given
        List<ChapterInfo> chapters = List.of(
            new ChapterInfo(1L, "Scorpio 지부"),
            new ChapterInfo(2L, "Ain 지부"),
            new ChapterInfo(3L, "Leo 지부")
        );

        given(getChapterUseCase.getAllChapters()).willReturn(chapters);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/chapters"));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.chapters").isArray())
            .andExpect(jsonPath("$.result.chapters[0].id").isString())
            .andExpect(jsonPath("$.result.chapters[*].id").value(contains("1", "2", "3")))
            .andExpect(jsonPath("$.result.chapters[*].name").value(contains("Scorpio 지부", "Ain 지부", "Leo 지부")));
    }
}
