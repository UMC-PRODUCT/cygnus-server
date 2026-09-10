package com.umc.product.curriculum.adapter.in.web.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = ChallengerWorkbookCommandV2Controller.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChallengerWorkbookCommandV2Controller")
class ChallengerWorkbookCommandV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ManageChallengerWorkbookUseCase manageChallengerWorkbookUseCase;

    @MockitoBean
    private ManageWeeklyBestWorkbookUseCase manageWeeklyBestWorkbookUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(99L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("제출이 연결된 챌린저 워크북 삭제는 409를 반환한다")
    void workbookWithSubmission_deleteReturnsConflict() throws Exception {
        doThrow(new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS))
            .when(manageChallengerWorkbookUseCase)
            .delete(any());

        mockMvc.perform(delete("/api/v2/curriculums/challenger-workbooks/{id}", 10L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS.getCode()));

        then(manageChallengerWorkbookUseCase).should().delete(any());
    }
}
