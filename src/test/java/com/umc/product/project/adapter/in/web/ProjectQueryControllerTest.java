package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.assembler.ProjectResponseAssembler;
import com.umc.product.project.adapter.in.web.dto.response.ProjectSummaryResponse;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;

@WebMvcTest(controllers = ProjectQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectQueryControllerTest {

    private static final Long TEST_MEMBER_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ProjectResponseAssembler assembler;

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
    void GET_프로젝트_목록_기본정렬은_createdAt_ASC_name_ASC() throws Exception {
        given(assembler.searchFor(any(SearchProjectQuery.class), eq(TEST_MEMBER_ID)))
            .willReturn(new PageResponse<ProjectSummaryResponse>(List.of(), 0, 20, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/projects")
                .param("gisuId", "1"))
            .andExpect(status().isOk());

        ArgumentCaptor<SearchProjectQuery> captor = ArgumentCaptor.forClass(SearchProjectQuery.class);
        then(assembler).should().searchFor(captor.capture(), eq(TEST_MEMBER_ID));

        List<Sort.Order> orders = captor.getValue().pageable().getSort().stream().toList();
        assertThat(orders)
            .extracting(Sort.Order::getProperty)
            .containsExactly("createdAt", "name");
        assertThat(orders)
            .allMatch(Sort.Order::isAscending);
    }

    @Test
    void GET_프로젝트_목록_비회원도_조회_가능() throws Exception {
        SecurityContextHolder.clearContext();
        given(assembler.searchFor(any(SearchProjectQuery.class), isNull()))
            .willReturn(new PageResponse<ProjectSummaryResponse>(List.of(), 0, 20, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/projects")
                .param("gisuId", "1"))
            .andExpect(status().isOk());

        then(assembler).should().searchFor(any(SearchProjectQuery.class), isNull());
    }
}
