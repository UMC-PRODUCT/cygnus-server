package com.umc.product.demoday.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.demoday.application.port.in.command.ResetDemodayTestDataUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayTestDataAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
@TestPropertySource(properties = "demoday.test-data-reset.enabled=true")
@DisplayName("DemodayTestDataAdminController")
class DemodayTestDataAdminControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final String RESET_PATH = "/api/v1/demoday/admin/test-data";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ResetDemodayTestDataUseCase resetDemodayTestDataUseCase;

    @BeforeEach
    void setUp() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("테스트 데이터를 초기화하면 200과 테이블별 삭제 건수를 반환한다")
    void resetTestData() throws Exception {
        // given
        DemodayTestDataResetInfo info = new DemodayTestDataResetInfo(2, 3, 4, 5, 6);
        given(resetDemodayTestDataUseCase.reset(MEMBER_ID)).willReturn(info);

        // when & then
        mockMvc.perform(delete(RESET_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.deletedPolls").value(2))
            .andExpect(jsonPath("$.result.deletedBooths").value(3))
            .andExpect(jsonPath("$.result.deletedEntryCodes").value(4))
            .andExpect(jsonPath("$.result.deletedStamps").value(5))
            .andExpect(jsonPath("$.result.deletedVotes").value(6));

        then(resetDemodayTestDataUseCase).should().reset(MEMBER_ID);
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한이 없으면 403과 DEMODAY-0600을 반환한다")
    void rejectResetWhenSystemAdminAccessIsDenied() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED))
            .given(resetDemodayTestDataUseCase)
            .reset(MEMBER_ID);

        // when & then
        mockMvc.perform(delete(RESET_PATH))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMODAY-0600"));

        then(resetDemodayTestDataUseCase).should().reset(MEMBER_ID);
    }

    @Test
    @DisplayName("회원이 아닌 데모데이 참여자 인증은 403으로 거부하고 초기화를 호출하지 않는다")
    void rejectResetForNonMemberPrincipal() throws Exception {
        // given
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("demoday-guest", null, List.of()));

        // when & then
        mockMvc.perform(delete(RESET_PATH))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("COMMON-403"));

        then(resetDemodayTestDataUseCase).shouldHaveNoInteractions();
    }
}
