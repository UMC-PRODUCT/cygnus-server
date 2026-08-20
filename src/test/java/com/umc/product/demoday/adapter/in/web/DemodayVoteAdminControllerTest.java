package com.umc.product.demoday.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.demoday.application.port.in.command.ChangeDemodayVoteStatusUseCase;
import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayVoteStatusCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;
import com.umc.product.demoday.application.port.in.query.ListDemodayAdminVoteUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo.ParticipantInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.ListDemodayAdminVoteQuery;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayVoteAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
@DisplayName("DemodayVoteAdminController")
class DemodayVoteAdminControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long VOTE_ID = 9000L;
    private static final Instant VOTED_AT = Instant.parse("2026-08-15T10:00:00Z");
    private static final Instant REVOKED_AT = Instant.parse("2026-08-15T10:10:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private ListDemodayAdminVoteUseCase listDemodayAdminVoteUseCase;
    @MockitoBean private ChangeDemodayVoteStatusUseCase changeDemodayVoteStatusUseCase;

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
    @DisplayName("투표 기록을 조회하면 참여자·부스·상태와 커서를 반환한다")
    void listVotes() throws Exception {
        // given
        DemodayAdminVoteInfo vote = new DemodayAdminVoteInfo(
            VOTE_ID,
            VOTED_AT,
            new ParticipantInfo(DemodayParticipantType.MEMBER, "이재원"),
            new DemodayBoothInfo(20L, 101L, "잇픽"),
            DemodayVoteStatus.REVOKED,
            REVOKED_AT);

        given(listDemodayAdminVoteUseCase.listVotes(any(ListDemodayAdminVoteQuery.class)))
            .willReturn(new DemodayAdminVoteListInfo(POLL_ID, List.of(vote), VOTE_ID, true));

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/votes", POLL_ID)
                .param("cursor", "9100")
                .param("size", "20")
                .param("boothId", "20")
                .param("participantName", "재원"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
            .andExpect(jsonPath("$.result.content[0].voteId").value(VOTE_ID))
            .andExpect(jsonPath("$.result.content[0].participant.type").value("MEMBER"))
            .andExpect(jsonPath("$.result.content[0].participant.displayName").value("이재원"))
            .andExpect(jsonPath("$.result.content[0].booth.boothId").value(20))
            .andExpect(jsonPath("$.result.content[0].booth.displayName").value("잇픽"))
            .andExpect(jsonPath("$.result.content[0].status").value("REVOKED"))
            .andExpect(jsonPath("$.result.nextCursor").value(VOTE_ID))
            .andExpect(jsonPath("$.result.hasNext").value(true));

        then(listDemodayAdminVoteUseCase).should().listVotes(
            new ListDemodayAdminVoteQuery(POLL_ID, MEMBER_ID, 9100L, 20, 20L, "재원")
        );
    }

    @Test
    @DisplayName("투표 기록 조회 권한이 없으면 403을 반환한다")
    void rejectListWhenAdminAccessIsDenied() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED))
            .given(listDemodayAdminVoteUseCase)
            .listVotes(any(ListDemodayAdminVoteQuery.class));

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/votes", POLL_ID))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("표를 무효화하면 201과 변경된 행 상태를 반환한다")
    void revokeVote() throws Exception {
        // given
        given(changeDemodayVoteStatusUseCase.revoke(any(ChangeDemodayVoteStatusCommand.class)))
            .willReturn(new DemodayVoteStatusInfo(VOTE_ID, DemodayVoteStatus.REVOKED, REVOKED_AT));

        // when & then
        mockMvc.perform(post(
                "/api/v1/demoday/admin/polls/{pollId}/votes/{voteId}/revocation",
                POLL_ID,
                VOTE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"  중복 투표로 확인됨  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.voteId").value(VOTE_ID))
            .andExpect(jsonPath("$.result.status").value("REVOKED"))
            .andExpect(jsonPath("$.result.revokedAt").exists());

        then(changeDemodayVoteStatusUseCase).should().revoke(
            new ChangeDemodayVoteStatusCommand(POLL_ID, VOTE_ID, MEMBER_ID, "중복 투표로 확인됨")
        );
    }

    @Test
    @DisplayName("무효 해제 사유가 공백이면 400이고 유스케이스를 호출하지 않는다")
    void rejectRestoreWhenReasonIsBlank() throws Exception {
        // when & then
        mockMvc.perform(post(
                "/api/v1/demoday/admin/polls/{pollId}/votes/{voteId}/restoration",
                POLL_ID,
                VOTE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"   \"}"))
            .andExpect(status().isBadRequest());

        then(changeDemodayVoteStatusUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("목록 크기가 허용 범위를 벗어나면 400이고 유스케이스를 호출하지 않는다")
    void rejectInvalidPageSize() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/votes", POLL_ID).param("size", "0"))
            .andExpect(status().isBadRequest());

        then(listDemodayAdminVoteUseCase).shouldHaveNoInteractions();
    }
}
