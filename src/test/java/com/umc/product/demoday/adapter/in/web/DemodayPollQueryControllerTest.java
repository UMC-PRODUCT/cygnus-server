package com.umc.product.demoday.adapter.in.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.demoday.adapter.in.web.support.DemodayParticipantResolverConfig;
import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteReceiptInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayPollQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, DemodayParticipantResolverConfig.class})
@DisplayName("DemodayPollQueryController")
class DemodayPollQueryControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final int BOOTH_CODE = 11;
    public static final int STAMP_COUNT = 6;
    public static final int REQUIRED_STAMP_COUNT = 6;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private ListDemodayPollUseCase listDemodayPollUseCase;

    @MockitoBean private GetDemodayParticipationUseCase getDemodayParticipationUseCase;

    @MockitoBean private ListDemodayBoothUseCase listDemodayBoothUseCase;

    @MockitoBean
    private DemodayParticipantResolver<MemberPrincipal> participantResolver;

    @MockitoBean
    private DemodayParticipantResolver<DemodayParticipationPrincipal> guestDemodayParticipantResolver;

    private MemberPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("데모데이 투표 목록을 조회한다")
    void listPolls() throws Exception {
        // given
        DemodayPollInfo pollInfo = getPollInfo();

        given(listDemodayPollUseCase.listPolls()).willReturn(List.of(pollInfo));

        // when & then
        mockMvc.perform(get("/api/v1/demoday/polls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.polls[0].pollId").value(POLL_ID))
                .andExpect(jsonPath("$.result.polls[0].name").value("PRODUCT 데모데이"))
                .andExpect(jsonPath("$.result.polls[0].status").value("OPEN"));

        then(listDemodayPollUseCase).should().listPolls();
    }

    @Test
    @DisplayName("회원의 투표 참여 정보를 조회한다")
    void getMyParticipation() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        List<DemodayStampInfo> demodayStampInfos = List.of(
            new DemodayStampInfo(20L, Instant.parse("2026-08-16T01:00:00Z")));

        DemodayParticipationInfo participationInfo = getParticipationInfo(demodayStampInfos);

        given(participantResolver.resolve(principal)).willReturn(participant);
        given(getDemodayParticipationUseCase.getParticipation(POLL_ID, participant))
                .willReturn(participationInfo);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/polls/{pollId}/participations/me", POLL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
                .andExpect(jsonPath("$.result.participantType").value("MEMBER"))
                .andExpect(jsonPath("$.result.stampCount").value(6))
                .andExpect(jsonPath("$.result.requiredStampCount").value(6))
                .andExpect(jsonPath("$.result.stamps[0].boothId").value(20L))
                .andExpect(jsonPath("$.result.hasActiveVote").value(false))
                .andExpect(jsonPath("$.result.hasUsedVoteSlot").value(false))
                .andExpect(jsonPath("$.result.canRequestVoteAuthorization").value(true))
                .andExpect(jsonPath("$.result.activeVoteReceipt").value(nullValue()));

        then(participantResolver).should().resolve(principal);
        then(getDemodayParticipationUseCase).should().getParticipation(POLL_ID, participant);
    }

    @Test
    @DisplayName("유효한 표가 있으면 참여 정보에 최종 투표 영수증을 반환한다")
    void getMyParticipationWithActiveVoteReceipt() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBoothInfo selectedBooth = new DemodayBoothInfo(20L, BOOTH_CODE, 30L, "PRODUCT 프로젝트");
        DemodayVoteReceiptInfo receipt = new DemodayVoteReceiptInfo(
            100L,
            selectedBooth,
            Instant.parse("2026-08-21T01:00:00Z")
        );
        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID,
            DemodayParticipantType.MEMBER,
            STAMP_COUNT,
            REQUIRED_STAMP_COUNT,
            List.of(),
            null,
            true,
            true,
            false,
            receipt
        );

        given(participantResolver.resolve(principal)).willReturn(participant);
        given(getDemodayParticipationUseCase.getParticipation(POLL_ID, participant))
            .willReturn(participationInfo);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/polls/{pollId}/participations/me", POLL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.hasActiveVote").value(true))
            .andExpect(jsonPath("$.result.hasUsedVoteSlot").value(true))
            .andExpect(jsonPath("$.result.canRequestVoteAuthorization").value(false))
            .andExpect(jsonPath("$.result.activeVoteReceipt.voteId").value(100L))
            .andExpect(jsonPath("$.result.activeVoteReceipt.selectedBooth.boothId").value(20L))
            .andExpect(jsonPath("$.result.activeVoteReceipt.selectedBooth.boothCode").value(BOOTH_CODE))
            .andExpect(jsonPath("$.result.activeVoteReceipt.selectedBooth.projectId").value(30L))
            .andExpect(jsonPath("$.result.activeVoteReceipt.votedAt").value("2026-08-21T01:00:00Z"));

        then(getDemodayParticipationUseCase).should().getParticipation(POLL_ID, participant);
    }

    @Test
    @DisplayName("투표 가능한 프로젝트 부스 목록을 조회한다")
    void listBooths() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        DemodayBoothInfo boothInfo = new DemodayBoothInfo(20L, BOOTH_CODE, 30L, "PRODUCT 프로젝트");
        given(participantResolver.resolve(principal)).willReturn(participant);
        given(listDemodayBoothUseCase.listBooths(POLL_ID, participant))
            .willReturn(List.of(boothInfo));

        // when & then
        mockMvc.perform(get("/api/v1/demoday/polls/{pollId}/booths", POLL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.booths[0].boothId").value(20L))
                .andExpect(jsonPath("$.result.booths[0].boothCode").value(BOOTH_CODE))
                .andExpect(jsonPath("$.result.booths[0].projectId").value(30L))
                .andExpect(jsonPath("$.result.booths[0].displayName").value("PRODUCT 프로젝트"));

        then(participantResolver).should().resolve(principal);
        then(listDemodayBoothUseCase).should().listBooths(POLL_ID, participant);
    }

    private static DemodayPollInfo getPollInfo() {
        return new DemodayPollInfo(
            POLL_ID,
            "PRODUCT 데모데이",
            Instant.parse("2026-08-16T00:00:00Z"),
            Instant.parse("2026-08-17T00:00:00Z"),
            DemodayPollStatus.OPEN);
    }

    private static DemodayParticipationInfo getParticipationInfo(List<DemodayStampInfo> demodayStampInfos) {
        return new DemodayParticipationInfo(
            POLL_ID,
            DemodayParticipantType.MEMBER,
            STAMP_COUNT,
            REQUIRED_STAMP_COUNT,
            demodayStampInfos,
            null,
            false,
            false,
            true,
            null);
    }
}
