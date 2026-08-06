package com.umc.product.challenger.adapter.in.web.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchItemV2Info;
import com.umc.product.member.application.port.in.query.dto.ChallengerSearchV2Result;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

@WebMvcTest(controllers = ChallengerSearchV2Controller.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChallengerSearchV2Controller")
class ChallengerSearchV2ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

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
    @DisplayName("챌린저 검색 v2 응답은 이메일을 마스킹한다")
    void 챌린저_검색_v2_응답은_이메일을_마스킹한다() throws Exception {
        given(searchMemberUseCase.searchChallengersByV2(any(), any(), any()))
            .willReturn(new ChallengerSearchV2Result(
                new PageImpl<>(
                    List.of(new ChallengerSearchItemV2Info(
                        1L, "홍길동", "길동", "gildong@example.com",
                        10L, "테스트대학교", null, 100L, 9L, 10L,
                        ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE, List.of(), false
                    )),
                    PageRequest.of(0, 10),
                    1
                )
            ));

        mockMvc.perform(get("/api/v2/challenger/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.page.content[0].email").value("gil****@example.com"));
    }

    @Test
    @DisplayName("챌린저 검색 v2는 현재 로그인 memberId를 별도 인자로 전달한다")
    void 챌린저_검색_v2는_현재_로그인_memberId를_별도_인자로_전달한다() throws Exception {
        given(searchMemberUseCase.searchChallengersByV2(any(), any(), any()))
            .willReturn(new ChallengerSearchV2Result(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
            ));

        mockMvc.perform(get("/api/v2/challenger/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());

        then(searchMemberUseCase).should().searchChallengersByV2(any(), eq(99L), any());
    }

    @Test
    @DisplayName("챌린저 검색 v2 권한이 없으면 403을 반환한다")
    void 챌린저_검색_v2_권한이_없으면_403을_반환한다() throws Exception {
        given(searchMemberUseCase.searchChallengersByV2(any(), any(), any()))
            .willThrow(new MemberDomainException(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED));

        mockMvc.perform(get("/api/v2/challenger/search")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isForbidden());
    }
}
