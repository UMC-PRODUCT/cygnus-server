package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.community.application.port.in.command.thread.report.ReportCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.report.SearchCommunityThreadMessageReportsUseCase;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageAdminReportInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportPageInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportReceiptInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.SearchCommunityThreadMessageReportsQuery;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = CommunityThreadMessageReportController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Community thread message 신고 REST adapter")
class CommunityThreadMessageReportControllerTest {

    private static final Long REQUESTER_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ReportCommunityThreadMessageUseCase reportMessageUseCase;

    @MockitoBean
    private SearchCommunityThreadMessageReportsUseCase searchReportsUseCase;

    @BeforeEach
    void setUpCurrentMember() {
        MemberPrincipal principal = new MemberPrincipal(REQUESTER_ID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("message 신고는 CurrentMember와 reason만 Community Port In에 전달한다")
    void reportMessage_usesCurrentMemberAndReturnsPublicReceipt() throws Exception {
        given(reportMessageUseCase.report(any())).willReturn(
            new CommunityThreadMessageReportReceiptInfo(700L, 500L, ReportReason.ABUSE, NOW)
        );

        mockMvc.perform(post("/api/v1/community/messages/500/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"ABUSE"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.reportId").value("700"))
            .andExpect(jsonPath("$.result.messageId").value("500"))
            .andExpect(jsonPath("$.result.reason").value("ABUSE"))
            .andExpect(jsonPath("$.result.reporterId").doesNotExist());

        ArgumentCaptor<ReportCommunityThreadMessageCommand> captor =
            ArgumentCaptor.forClass(ReportCommunityThreadMessageCommand.class);
        then(reportMessageUseCase).should().report(captor.capture());
        assertThat(captor.getValue().messageId()).isEqualTo(500L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().reason()).isEqualTo(ReportReason.ABUSE);
    }

    @Test
    @DisplayName("admin inbox filter와 문자열 reporter ID를 보존한다")
    void searchReports_mapsFiltersAndNumericValues() throws Exception {
        given(searchReportsUseCase.search(any())).willReturn(new CommunityThreadMessageReportPageInfo(
            List.of(new CommunityThreadMessageAdminReportInfo(
                700L, 42L, 500L, ReportReason.SPAM, ReportStatus.PENDING, 88L, NOW
            )), 20, 21L
        ));

        mockMvc.perform(get("/api/v1/community/admin/thread-message-reports")
                .param("reason", "SPAM")
                .param("threadId", "42")
                .param("reporterId", "88"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].reportId").value("700"))
            .andExpect(jsonPath("$.result.items[0].threadId").value("42"))
            .andExpect(jsonPath("$.result.items[0].messageId").value("500"))
            .andExpect(jsonPath("$.result.items[0].reporterId").value("88"))
            .andExpect(jsonPath("$.result.nextOffset").value("20"))
            .andExpect(jsonPath("$.result.total").value("21"));

        ArgumentCaptor<SearchCommunityThreadMessageReportsQuery> captor =
            ArgumentCaptor.forClass(SearchCommunityThreadMessageReportsQuery.class);
        then(searchReportsUseCase).should().search(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().status()).isEqualTo(ReportStatus.PENDING);
        assertThat(captor.getValue().reason()).isEqualTo(ReportReason.SPAM);
        assertThat(captor.getValue().threadId()).isEqualTo(42L);
        assertThat(captor.getValue().reporterMemberId()).isEqualTo(88L);
        assertThat(captor.getValue().offset()).isZero();
        assertThat(captor.getValue().limit()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "+1", "01", "1x", "9223372036854775808"})
    @DisplayName("positive-decimal full match가 아닌 message ID는 신고 Port In 전에 거절한다")
    void reportMessage_rejectsInvalidPositiveDecimalId(String messageId) throws Exception {
        mockMvc.perform(post("/api/v1/community/messages/{messageId}/report", messageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"SPAM"}
                    """))
            .andExpect(status().is4xxClientError());

        then(reportMessageUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reason\":null}", "{\"reason\":\"UNKNOWN\"}"})
    @DisplayName("누락, null, 미지원 report reason은 Port In 전에 거절한다")
    void reportMessage_rejectsInvalidReason(String body) throws Exception {
        mockMvc.perform(post("/api/v1/community/messages/500/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        then(reportMessageUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "+1", "01", "x", "9223372036854775808"})
    @DisplayName("admin numeric filter는 positive-decimal full match만 허용한다")
    void searchReports_rejectsInvalidNumericFilter(String value) throws Exception {
        mockMvc.perform(get("/api/v1/community/admin/thread-message-reports")
                .param("reporterId", value))
            .andExpect(status().isBadRequest());

        then(searchReportsUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({"offset,-1", "limit,101"})
    @DisplayName("admin pagination 범위를 벗어나면 Port In 전에 거절한다")
    void searchReports_rejectsInvalidPagination(String parameter, String value) throws Exception {
        mockMvc.perform(get("/api/v1/community/admin/thread-message-reports")
                .param(parameter, value))
            .andExpect(status().isBadRequest());

        then(searchReportsUseCase).shouldHaveNoInteractions();
    }
}
