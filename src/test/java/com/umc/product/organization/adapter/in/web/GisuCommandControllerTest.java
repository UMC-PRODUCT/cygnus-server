package com.umc.product.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.adapter.in.web.dto.request.CreateGisuRequest;
import com.umc.product.support.ControllerTestSupport;

class GisuCommandControllerTest extends ControllerTestSupport {

    private static final Instant START_AT = Instant.parse("2025-03-01T00:00:00Z");
    private static final Instant END_AT = Instant.parse("2025-08-31T23:59:59Z");

    @Test
    void 신규_기수를_추가한다() throws Exception {
        // given
        CreateGisuRequest request = new CreateGisuRequest(9L, START_AT, END_AT);

        given(manageGisuUseCase.create(any())).willReturn(1L);

        // when
        ResultActions result = mockMvc.perform(
            post("/api/v1/gisu")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result").value("1"));
        then(manageGisuUseCase).should().create(request.toCommand());
    }

    @Test
    void 기수를_삭제한다() throws Exception {
        // given
        Long gisuId = 1L;

        // when
        ResultActions result = mockMvc.perform(delete("/api/v1/gisu/{gisuId}", gisuId));

        // then
        result.andExpect(status().isOk());
        then(manageGisuUseCase).should().deleteGisu(gisuId);
    }

    @Test
    void 현재_기수를_설정한다() throws Exception {
        // given
        Long gisuId = 3L;

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/gisu/{gisuId}/active", gisuId));

        // then
        result.andExpect(status().isOk());
        then(manageGisuUseCase).should().updateActiveGisu(gisuId);
    }
}
