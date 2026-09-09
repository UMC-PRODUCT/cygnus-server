package com.umc.product.organization.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.adapter.in.web.dto.request.CreateChapterRequest;
import com.umc.product.support.ControllerTestSupport;

class ChapterCommandControllerTest extends ControllerTestSupport {

    @Test
    void 신규_지부를_생성한다() throws Exception {
        // given
        CreateChapterRequest request = new CreateChapterRequest(1L, "Scorpio", List.of());
        given(manageChapterUseCase.create(request.toCommand())).willReturn(1L);

        // when
        ResultActions result = mockMvc.perform(
            post("/api/v1/chapters").content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result").value("1"));
        then(manageChapterUseCase).should().create(request.toCommand());
    }
}
