package com.umc.product.organization.adapter.in.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.ResultActions;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuNameInfo;
import com.umc.product.support.ControllerTestSupport;

class GisuQueryControllerTest extends ControllerTestSupport {

    @Test
    void 기수_목록을_페이징_조회한다() throws Exception {
        // given
        int page = 0;
        int size = 10;

        List<GisuInfo> gisuList = List.of(
            gisuInfo(3L, 9L, Instant.parse("2025-03-01T00:00:00Z"), Instant.parse("2025-08-31T23:59:59Z"), true),
            gisuInfo(2L, 8L, Instant.parse("2024-09-01T00:00:00Z"), Instant.parse("2025-02-28T23:59:59Z"), false),
            gisuInfo(1L, 7L, Instant.parse("2024-03-01T00:00:00Z"), Instant.parse("2024-08-31T23:59:59Z"), false));

        Page<GisuInfo> pageResult = new PageImpl<>(
            gisuList,
            PageRequest.of(page, size),
            3L
        );

        given(getGisuUseCase.getList(any())).willReturn(pageResult);

        // when
        ResultActions result = mockMvc.perform(
            get("/api/v1/gisu")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
        );

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.content").isArray())
            .andExpect(jsonPath("$.result.content[0].gisuId").isString())
            .andExpect(jsonPath("$.result.content[*].gisuId").value(contains("3", "2", "1")))
            .andExpect(jsonPath("$.result.content[*].generation").value(contains("9", "8", "7")))
            .andExpect(jsonPath("$.result.content[*].gisu").value(contains("9", "8", "7")))
            .andExpect(jsonPath("$.result.content[0].startAt").value("2025-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.result.content[0].endAt").value("2025-08-31T23:59:59Z"))
            .andExpect(jsonPath("$.result.content[*].isActive").value(contains(true, false, false)))
            .andExpect(jsonPath("$.result.page").value("0"))
            .andExpect(jsonPath("$.result.size").value("10"))
            .andExpect(jsonPath("$.result.totalElements").value("3"))
            .andExpect(jsonPath("$.result.totalPages").value("1"))
            .andExpect(jsonPath("$.result.hasNext").value(false))
            .andExpect(jsonPath("$.result.hasPrevious").value(false));
    }

    @Test
    void 기수_전체_목록을_조회한다() throws Exception {
        // given
        List<GisuNameInfo> gisuNames = List.of(
            gisuNameInfo(3L, 9L, true),
            gisuNameInfo(2L, 8L, false),
            gisuNameInfo(1L, 7L, false)
        );

        given(getGisuUseCase.getAllGisuNames()).willReturn(gisuNames);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/gisu/all"));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.gisuList").isArray())
            .andExpect(jsonPath("$.result.gisuList[0].gisuId").isString())
            .andExpect(jsonPath("$.result.gisuList[*].gisuId").value(contains("3", "2", "1")))
            .andExpect(jsonPath("$.result.gisuList[*].generation").value(contains("9", "8", "7")))
            .andExpect(jsonPath("$.result.gisuList[*].gisu").value(contains("9", "8", "7")))
            .andExpect(jsonPath("$.result.gisuList[*].isActive").value(contains(true, false, false)));
    }

    @Test
    void 활성화된_기수를_조회한다() throws Exception {
        // given
        GisuInfo activeGisu = gisuInfo(3L, 9L, Instant.parse("2025-03-01T00:00:00Z"),
            Instant.parse("2025-08-31T23:59:59Z"), true);
        given(getGisuUseCase.getActiveGisu()).willReturn(activeGisu);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/gisu/active"));

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.gisuId").isString())
            .andExpect(jsonPath("$.result.gisuId").value("3"))
            .andExpect(jsonPath("$.result.generation").value("9"))
            .andExpect(jsonPath("$.result.gisu").value("9"));
    }

    private GisuInfo gisuInfo(Long id, Long generation, Instant startAt, Instant endAt, boolean isActive) {
        return new GisuInfo(id, generation, startAt, endAt, isActive);
    }

    private GisuNameInfo gisuNameInfo(Long id, Long generation, boolean isActive) {
        return new GisuNameInfo(id, generation, generation, isActive);
    }
}
