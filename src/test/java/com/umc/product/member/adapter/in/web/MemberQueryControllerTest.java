package com.umc.product.member.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.adapter.in.web.assembler.MemberInfoResponseAssembler;
import com.umc.product.member.adapter.in.web.dto.response.MemberInfoResponse;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberResult;

@WebMvcTest(controllers = MemberQueryController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MemberQueryController")
class MemberQueryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    MemberInfoResponseAssembler assembler;

    @MockitoBean
    SearchMemberUseCase searchMemberUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(99L)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("내 프로필을 조회한다")
    void 내_프로필을_조회한다() throws Exception {
        given(assembler.fromMemberId(99L)).willReturn(MemberInfoResponse.builder()
            .id(99L)
            .name("홍길동")
            .status(MemberStatus.ACTIVE)
            .build());

        mockMvc.perform(get("/api/v1/member/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(99L))
            .andExpect(jsonPath("$.result.name").value("홍길동"));
    }

    @Test
    @DisplayName("회원 검색 응답은 이메일을 마스킹한다")
    void 회원_검색_응답은_이메일을_마스킹한다() throws Exception {
        given(searchMemberUseCase.searchBy(any(), any())).willReturn(new SearchMemberResult(
            new PageImpl<>(
                List.of(new SearchMemberItemInfo(
                    1L, "홍길동", "길동", "gildong@example.com",
                    10L, "테스트대학교", null, 100L, 9L, 10L,
                    ChallengerPart.SPRINGBOOT, List.of()
                )),
                PageRequest.of(0, 10),
                1
            )
        ));

        mockMvc.perform(get("/api/v1/member/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.page.content[0].email").value("gil****@example.com"));
    }

    @Test
    @DisplayName("회원 검색은 현재 로그인 memberId를 Query에 포함한다")
    void 회원_검색은_현재_로그인_memberId를_Query에_포함한다() throws Exception {
        given(searchMemberUseCase.searchBy(any(), any())).willReturn(new SearchMemberResult(
            new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        ));

        mockMvc.perform(get("/api/v1/member/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberUseCase).should().searchBy(queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue().requesterMemberId()).isEqualTo(99L);
    }
}
