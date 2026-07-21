package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@WebMvcTest(RecruitingApplicationReviewController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RecruitingApplicationReviewController")
class RecruitingApplicationReviewControllerTest {

    private static final Long REQUESTER_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    SearchRecruitingApplicationUseCase searchApplicationUseCase;

    @BeforeEach
    void authenticate() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(REQUESTER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("평가용 지원서 목록은 Round, 상태, 트랙, 페이지와 CurrentMember를 전달한다")
    void searchApplicationsWithFilters() throws Exception {
        given(searchApplicationUseCase.search(any())).willReturn(new PageImpl<>(
            List.of(summary()),
            PageRequest.of(1, 10),
            11
        ));

        mockMvc.perform(get("/api/v1/recruiting/rounds/{roundId}/applications", 20L)
                .param("status", "SUBMITTED")
                .param("track", "PLAN")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].applicationId").value(30L))
            .andExpect(jsonPath("$.result.content[0].documentEvaluatedByMe").value(true))
            .andExpect(jsonPath("$.result.totalElements").value(11L));

        ArgumentCaptor<RecruitingApplicationSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingApplicationSearchQuery.class);
        then(searchApplicationUseCase).should().search(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(20L);
        assertThat(captor.getValue().status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.PLAN);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().pageable().getPageNumber()).isEqualTo(1);
    }

    private RecruitingApplicationSummaryInfo summary() {
        return RecruitingApplicationSummaryInfo.builder()
            .applicationId(30L)
            .applicantName("지원자")
            .email("applicant@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .status(RecruitingApplicationStatus.SUBMITTED)
            .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
            .submittedAt(Instant.parse("2026-08-07T00:00:00Z"))
            .documentEvaluatedByMe(true)
            .build();
    }
}
