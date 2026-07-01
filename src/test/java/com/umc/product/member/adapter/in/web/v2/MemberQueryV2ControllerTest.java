package com.umc.product.member.adapter.in.web.v2;

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
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberSummaryV2UseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberProfileInfo;
import com.umc.product.member.application.port.in.query.dto.MemberSummaryV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;

@WebMvcTest(controllers = MemberQueryV2Controller.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MemberQueryV2Controller")
class MemberQueryV2ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetMemberSummaryV2UseCase getMemberSummaryV2UseCase;

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
    @DisplayName("내 v2 종합 정보를 조회한다")
    void 내_v2_종합_정보를_조회한다() throws Exception {
        given(getMemberSummaryV2UseCase.getSummaryByMemberId(99L)).willReturn(new MemberSummaryV2Info(
            MemberInfo.builder()
                .id(99L)
                .name("홍길동")
                .nickname("길동")
                .email("gildong@example.com")
                .schoolId(1L)
                .schoolName("테스트대학교")
                .status(MemberStatus.ACTIVE)
                .build(),
            MemberProfileInfo.builder().github("https://github.com/umc").build(),
            true,
            30L,
            null,
            List.of()
        ));

        mockMvc.perform(get("/api/v2/member/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(99L))
            .andExpect(jsonPath("$.result.hasLocalCredential").value(true))
            .andExpect(jsonPath("$.result.totalActivityDays").value(30L));
    }

    @Test
    @DisplayName("회원 검색 v2 응답은 이메일을 마스킹한다")
    void 회원_검색_v2_응답은_이메일을_마스킹한다() throws Exception {
        given(searchMemberUseCase.searchByV2(any(), any())).willReturn(new SearchMemberV2Result(
            new PageImpl<>(
                List.of(new SearchMemberItemV2Info(
                    1L, "홍길동", "길동", "gildong@example.com",
                    10L, "테스트대학교", null,
                    new SearchMemberItemV2Info.PrimaryChallenger(
                        100L, 9L, 10L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE
                    ),
                    false,
                    List.of()
                )),
                PageRequest.of(0, 10),
                1
            )
        ));

        mockMvc.perform(get("/api/v2/member/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.page.content[0].email").value("gil****@example.com"));
    }

    @Test
    @DisplayName("회원 검색 v2는 현재 로그인 memberId를 Query에 포함한다")
    void 회원_검색_v2는_현재_로그인_memberId를_Query에_포함한다() throws Exception {
        given(searchMemberUseCase.searchByV2(any(), any())).willReturn(new SearchMemberV2Result(
            new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        ));

        mockMvc.perform(get("/api/v2/member/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberUseCase).should().searchByV2(queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue().requesterMemberId()).isEqualTo(99L);
    }
}
