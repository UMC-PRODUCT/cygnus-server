package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.assembler.ProjectApplicationResponseAssembler;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicantResponse;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;

@WebMvcTest(controllers = ProjectApplicationQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectApplicationQueryControllerTest {

    private static final Long TEST_MEMBER_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ProjectApplicationResponseAssembler assembler;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(TEST_MEMBER_ID)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("batch_지원자_목록_조회는_projectIds_중복을_제거하고_필터를_query에_전달한다")
    void batch_지원자_목록_조회_query_전달() throws Exception {
        // given
        Map<Long, List<ProjectApplicantResponse>> response = new LinkedHashMap<>();
        response.put(1L, List.of());
        response.put(2L, List.of());
        given(assembler.applicantsForBatch(any())).willReturn(response);

        // when
        mockMvc.perform(get("/api/v1/projects/applications")
                .param("projectIds", "1", "2", "1")
                .param("matchingRoundId", "7")
                .param("part", "WEB")
                .param("status", "APPROVED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result['1']").isArray())
            .andExpect(jsonPath("$.result['2']").isArray());

        // then
        ArgumentCaptor<SearchProjectApplicationsBatchQuery> captor =
            ArgumentCaptor.forClass(SearchProjectApplicationsBatchQuery.class);
        then(assembler).should().applicantsForBatch(captor.capture());
        SearchProjectApplicationsBatchQuery query = captor.getValue();
        assertThat(query.requesterMemberId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(query.projectIds()).containsExactly(1L, 2L);
        assertThat(query.matchingRoundId()).isEqualTo(7L);
        assertThat(query.part()).isEqualTo(ChallengerPart.WEB);
        assertThat(query.status()).isEqualTo(ProjectApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("batch_지원자_목록_조회는_projectIds가_없으면_400")
    void batch_지원자_목록_projectIds_누락_400() throws Exception {
        mockMvc.perform(get("/api/v1/projects/applications"))
            .andExpect(status().isBadRequest());

        then(assembler).should(never()).applicantsForBatch(any());
    }

    @Test
    @DisplayName("batch_지원자_목록_조회는_projectIds가_100개를_초과하면_400")
    void batch_지원자_목록_projectIds_100개_초과_400() throws Exception {
        var request = get("/api/v1/projects/applications");
        for (int i = 1; i <= 101; i++) {
            request.param("projectIds", String.valueOf(i));
        }

        mockMvc.perform(request)
            .andExpect(status().isBadRequest());

        then(assembler).should(never()).applicantsForBatch(any());
    }

    @Test
    @DisplayName("기존_단일_프로젝트_지원자_목록_경로는_유지된다")
    void 단일_프로젝트_지원자_목록_경로_유지() throws Exception {
        // given
        given(assembler.applicantsFor(any())).willReturn(List.of());

        // when
        mockMvc.perform(get("/api/v1/projects/{projectId}/applications", 42L)
                .param("matchingRoundId", "7")
                .param("part", "WEB")
                .param("status", "SUBMITTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").isArray());

        // then
        ArgumentCaptor<SearchProjectApplicationsQuery> captor =
            ArgumentCaptor.forClass(SearchProjectApplicationsQuery.class);
        then(assembler).should().applicantsFor(captor.capture());
        SearchProjectApplicationsQuery query = captor.getValue();
        assertThat(query.requesterMemberId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(query.projectId()).isEqualTo(42L);
        assertThat(query.matchingRoundId()).isEqualTo(7L);
        assertThat(query.part()).isEqualTo(ChallengerPart.WEB);
        assertThat(query.status()).isEqualTo(ProjectApplicationStatus.SUBMITTED);
    }
}
