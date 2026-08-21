package com.umc.product.demoday.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantCookieWriter;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantTokenProvider;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.demoday.adapter.in.web.support.DemodayParticipantResolverConfig;
import com.umc.product.demoday.application.port.in.command.CastDemodayVoteUseCase;
import com.umc.product.demoday.application.port.in.command.CollectDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayVoteAuthorizationUseCase;
import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CastDemodayVoteCommand;
import com.umc.product.demoday.application.port.in.command.dto.CollectDemodayStampCommand;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayVoteAuthorizationCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayParticipationCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, DemodayParticipantCookieWriter.class, DemodayParticipantResolverConfig.class})
@DisplayName("DemodayParticipationCommandController")
class DemodayParticipationCommandControllerTest {

    private static final Long POLL_ID = 10L;
    private static final Long ENTRY_CODE_ID = 42L;
    private static final Long MEMBER_ID = 1L;
    private static final Long BOOTH_ID = 20L;
    private static final int BOOTH_CODE = 11;
    // MockHttpServletResponse의 Set-Cookie 파서(MockCookie#parse)는 Max-Age를 int로 파싱한다.
    // 실제 poll.closesAt은 항상 근시일이라 문제되지 않지만, 테스트 데이터는 그 한계를 넘지 않게 근접 미래로 둔다.
    private static final Instant CLOSES_AT = Instant.now().plusSeconds(3600);

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private StartDemodayGuestParticipationUseCase startDemodayGuestParticipationUseCase;

    @MockitoBean private CollectDemodayStampUseCase collectDemodayStampUseCase;

    @MockitoBean private CreateDemodayVoteAuthorizationUseCase createDemodayVoteAuthorizationUseCase;

    @MockitoBean private CastDemodayVoteUseCase castDemodayVoteUseCase;

    @MockitoBean private DemodayParticipantTokenProvider demodayParticipantTokenProvider;

    @MockitoBean private DemodayParticipantResolver<MemberPrincipal> memberDemodayParticipantResolver;

    @MockitoBean private DemodayParticipantResolver<DemodayParticipationPrincipal> guestDemodayParticipantResolver;

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
    @DisplayName("정상 코드를 제출하면 201과 함께 HttpOnly Cookie를 설정한다")
    void startGuestParticipation() throws Exception {
        // given
        given(demodayParticipantTokenProvider.parseEntryCodeId(null)).willReturn(Optional.empty());

        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID, DemodayParticipantType.GUEST, 0, 6,
            List.of(), null, false, false, false, null);

        StartDemodayGuestParticipationInfo info =
            new StartDemodayGuestParticipationInfo("issued-token", CLOSES_AT, participationInfo);

        given(startDemodayGuestParticipationUseCase.start(
            new StartDemodayGuestParticipationCommand(POLL_ID, "GUEST-A1B2C3", null)))
            .willReturn(info);

        // when
        MvcResult result = mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/participations/guest", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admissionCode\":\"GUEST-A1B2C3\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
            .andExpect(jsonPath("$.result.participantType").value("GUEST"))
            .andReturn();

        // then
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("demoday_participant_token=issued-token");
        assertThat(setCookieHeader).containsIgnoringCase("httponly");
        assertThat(setCookieHeader).containsIgnoringCase("secure");
        assertThat(setCookieHeader).containsIgnoringCase("samesite=lax");
        assertThat(setCookieHeader).doesNotContainIgnoringCase("domain=");
    }

    @Test
    @DisplayName("이미 유효한 Cookie가 있으면 파싱한 entryCodeId를 커맨드에 실어 보낸다")
    void startGuestParticipationWithExistingCookie() throws Exception {
        // given
        given(demodayParticipantTokenProvider.parseEntryCodeId("existing-token"))
            .willReturn(Optional.of(ENTRY_CODE_ID));

        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID, DemodayParticipantType.GUEST, 2, 6,
            List.of(), null, false, false, false, null);

        StartDemodayGuestParticipationInfo info =
            new StartDemodayGuestParticipationInfo("issued-token", CLOSES_AT, participationInfo);

        given(startDemodayGuestParticipationUseCase.start(
            new StartDemodayGuestParticipationCommand(POLL_ID, "GUEST-A1B2C3", ENTRY_CODE_ID)))
            .willReturn(info);

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/participations/guest", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admissionCode\":\"GUEST-A1B2C3\"}")
                .cookie(new jakarta.servlet.http.Cookie(
                    DemodayParticipantTokenProvider.COOKIE_NAME, "existing-token")))
            .andExpect(status().isCreated());

        then(startDemodayGuestParticipationUseCase).should().start(
            eq(new StartDemodayGuestParticipationCommand(POLL_ID, "GUEST-A1B2C3", ENTRY_CODE_ID)));
    }

    @Test
    @DisplayName("입장 코드가 비어 있으면 400을 반환한다")
    void startGuestParticipationWithBlankAdmissionCode() throws Exception {
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/participations/guest", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"admissionCode\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원이 부스 QR을 스캔하면 201과 함께 적립 결과를 반환한다")
    void collectStamp() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);

        DemodayStampCollectInfo info = new DemodayStampCollectInfo(
            new DemodayStampInfo(BOOTH_ID, Instant.parse("2026-08-19T10:00:00Z")),
            1, 6, null, false);
        given(collectDemodayStampUseCase.collect(
            new CollectDemodayStampCommand(POLL_ID, "sq_opaque_credential", participant)))
            .willReturn(info);

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/stamps", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCredential\":\"sq_opaque_credential\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.stamp.boothId").value(BOOTH_ID))
            .andExpect(jsonPath("$.result.stampCount").value(1))
            .andExpect(jsonPath("$.result.requiredStampCount").value(6))
            .andExpect(jsonPath("$.result.canRequestVoteAuthorization").value(false));

        then(collectDemodayStampUseCase).should().collect(
            eq(new CollectDemodayStampCommand(POLL_ID, "sq_opaque_credential", participant)));
    }

    @Test
    @DisplayName("qrCredential이 비어 있으면 400을 반환한다")
    void collectStampWithBlankCredential() throws Exception {
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/stamps", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrCredential\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("INFO QR과 선택 부스를 제출하면 201과 5분 투표 권한을 반환한다")
    void createVoteAuthorization() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);
        DemodayBoothInfo selectedBooth = new DemodayBoothInfo(BOOTH_ID, BOOTH_CODE, 101L, "선택 부스");
        given(createDemodayVoteAuthorizationUseCase.create(
            new CreateDemodayVoteAuthorizationCommand(POLL_ID, BOOTH_ID, "info-qr-token", participant)))
            .willReturn(new DemodayVoteAuthorizationInfo(
                "vote-authorization-token", selectedBooth, Instant.parse("2026-08-19T10:05:00Z")));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/vote-authorizations", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"boothId":20,"qrToken":"info-qr-token"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.voteAuthorizationToken").value("vote-authorization-token"))
            .andExpect(jsonPath("$.result.selectedBooth.boothId").value(BOOTH_ID))
            .andExpect(jsonPath("$.result.selectedBooth.boothCode").value(BOOTH_CODE))
            .andExpect(jsonPath("$.result.expiresAt").value("2026-08-19T10:05:00Z"));
    }

    @Test
    @DisplayName("선택 부스가 없으면 투표 권한 발급 요청을 400으로 거부한다")
    void createVoteAuthorizationWithoutBooth() throws Exception {
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/vote-authorizations", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\":\"info-qr-token\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투표 권한만 제출하면 201과 최종 표를 반환한다")
    void castVote() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);
        DemodayBoothInfo selectedBooth = new DemodayBoothInfo(BOOTH_ID, BOOTH_CODE, 101L, "선택 부스");
        given(castDemodayVoteUseCase.cast(
            new CastDemodayVoteCommand(POLL_ID, "vote-authorization-token", participant)))
            .willReturn(new DemodayVoteInfo(
                100L, POLL_ID, selectedBooth, Instant.parse("2026-08-19T10:01:00Z")));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/votes", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voteAuthorizationToken\":\"vote-authorization-token\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.voteId").value(100L))
            .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
            .andExpect(jsonPath("$.result.selectedBooth.boothId").value(BOOTH_ID))
            .andExpect(jsonPath("$.result.selectedBooth.boothCode").value(BOOTH_CODE))
            .andExpect(jsonPath("$.result.votedAt").value("2026-08-19T10:01:00Z"));
    }

    @Test
    @DisplayName("투표 권한이 비어 있으면 최종 제출을 400으로 거부한다")
    void castVoteWithBlankAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/votes", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voteAuthorizationToken\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("스탬프가 부족하면 투표 권한 발급 요청에 403과 원인 코드를 반환한다")
    void createVoteAuthorizationWithInsufficientStamps() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);
        given(createDemodayVoteAuthorizationUseCase.create(
            new CreateDemodayVoteAuthorizationCommand(POLL_ID, BOOTH_ID, "info-qr-token", participant)))
            .willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_INSUFFICIENT_STAMPS));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/vote-authorizations", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothId\":20,\"qrToken\":\"info-qr-token\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_VOTE_INSUFFICIENT_STAMPS.getCode()));
    }

    @Test
    @DisplayName("외부 부스를 선택하면 투표 권한 발급 요청에 409와 원인 코드를 반환한다")
    void createVoteAuthorizationForExternalBooth() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);
        given(createDemodayVoteAuthorizationUseCase.create(
            new CreateDemodayVoteAuthorizationCommand(POLL_ID, BOOTH_ID, "info-qr-token", participant)))
            .willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/vote-authorizations", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothId\":20,\"qrToken\":\"info-qr-token\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code")
                .value(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED.getCode()));
    }

    @Test
    @DisplayName("만료된 투표 권한은 최종 제출에 401과 원인 코드를 반환한다")
    void castVoteWithExpiredAuthorization() throws Exception {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(memberDemodayParticipantResolver.resolve(principal)).willReturn(participant);
        given(castDemodayVoteUseCase.cast(
            new CastDemodayVoteCommand(POLL_ID, "expired-token", participant)))
            .willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_EXPIRED));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/polls/{pollId}/votes", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voteAuthorizationToken\":\"expired-token\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_EXPIRED.getCode()));
    }
}
