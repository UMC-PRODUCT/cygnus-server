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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantCookieWriter;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantTokenProvider;
import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = DemodayParticipationCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, DemodayParticipantCookieWriter.class})
@DisplayName("DemodayParticipationCommandController")
class DemodayParticipationCommandControllerTest {

    private static final Long POLL_ID = 10L;
    private static final Long ENTRY_CODE_ID = 42L;
    // MockHttpServletResponse의 Set-Cookie 파서(MockCookie#parse)는 Max-Age를 int로 파싱한다.
    // 실제 poll.closesAt은 항상 근시일이라 문제되지 않지만, 테스트 데이터는 그 한계를 넘지 않게 근접 미래로 둔다.
    private static final Instant CLOSES_AT = Instant.now().plusSeconds(3600);

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private StartDemodayGuestParticipationUseCase startDemodayGuestParticipationUseCase;

    @MockitoBean private DemodayParticipantTokenProvider demodayParticipantTokenProvider;

    @Test
    @DisplayName("정상 코드를 제출하면 201과 함께 HttpOnly Cookie를 설정한다")
    void startGuestParticipation() throws Exception {
        // given
        given(demodayParticipantTokenProvider.parseEntryCodeId(null)).willReturn(Optional.empty());

        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID, DemodayParticipantType.GUEST, 0, 6,
            List.of(), null, false, false);

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
            List.of(), null, false, false);

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
}
