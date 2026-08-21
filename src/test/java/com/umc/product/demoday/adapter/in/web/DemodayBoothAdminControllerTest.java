package com.umc.product.demoday.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.demoday.application.port.in.command.CreateDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.RegisterDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand.BoothRegistration;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothCommand;
import com.umc.product.demoday.application.port.in.query.ListDemodayAdminBoothUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayBoothAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
@DisplayName("DemodayBoothAdminController")
class DemodayBoothAdminControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long PROJECT_ID = 101L;
    private static final Long BOOTH_ID = 20L;
    private static final int BOOTH_CODE = 11;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private RegisterDemodayBoothUseCase registerDemodayBoothUseCase;

    @MockitoBean private ListDemodayAdminBoothUseCase listDemodayAdminBoothUseCase;

    @MockitoBean private CreateDemodayStampUseCase createDemodayStampUseCase;

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
    @DisplayName("projectId로 부스를 등록하면 201과 부스 ID를 반환한다")
    void registerProjectBooth() throws Exception {
        // given
        given(registerDemodayBoothUseCase.register(any(RegisterDemodayBoothCommand.class)))
            .willReturn(BOOTH_ID);

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": 11, \"projectId\": 101}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.boothId").value(BOOTH_ID));

        then(registerDemodayBoothUseCase).should()
            .register(new RegisterDemodayBoothCommand(MEMBER_ID, POLL_ID, BOOTH_CODE, PROJECT_ID, null));
    }

    @Test
    @DisplayName("displayName으로 외부 부스를 등록하면 201과 부스 ID를 반환한다")
    void registerExternalBooth() throws Exception {
        // given
        given(registerDemodayBoothUseCase.register(any(RegisterDemodayBoothCommand.class)))
            .willReturn(BOOTH_ID);

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": 11, \"displayName\": \"외부 참가팀 A\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.boothId").value(BOOTH_ID));

        then(registerDemodayBoothUseCase).should()
            .register(new RegisterDemodayBoothCommand(MEMBER_ID, POLL_ID, BOOTH_CODE, null, "외부 참가팀 A"));
    }

    @Test
    @DisplayName("투표가 OPEN이면 부스 등록은 409와 잠금 코드를 반환한다")
    void returnConflictWhenBoothLocked() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED))
            .given(registerDemodayBoothUseCase)
            .register(any(RegisterDemodayBoothCommand.class));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": 11, \"projectId\": 101}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED.getCode()));
    }

    @Test
    @DisplayName("같은 Poll의 중복 코드를 등록하면 409와 중복 코드를 반환한다")
    void returnConflictWhenBoothCodeIsDuplicated() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED))
            .given(registerDemodayBoothUseCase)
            .register(any(RegisterDemodayBoothCommand.class));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": 11, \"projectId\": 101}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED.getCode()));
    }

    @Test
    @DisplayName("부스 코드가 누락되면 400을 반환하고 유스케이스를 호출하지 않는다")
    void rejectMissingBoothCode() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 101}"))
            .andExpect(status().isBadRequest());

        then(registerDemodayBoothUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "boothCode={0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("부스 코드가 양수가 아니면 400을 반환하고 유스케이스를 호출하지 않는다")
    void rejectNonPositiveBoothCode(int boothCode) throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": %d, \"projectId\": 101}".formatted(boothCode)))
            .andExpect(status().isBadRequest());

        then(registerDemodayBoothUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록 경로를 정할 수 없는 요청은 400과 식별자 오류 코드를 반환한다")
    void returnBadRequestWhenIdentifierIsAmbiguous() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER))
            .given(registerDemodayBoothUseCase)
            .register(any(RegisterDemodayBoothCommand.class));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boothCode\": 11, \"projectId\": 101, \"displayName\": \"외부 참가팀 A\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER.getCode()));
    }

    @Test
    @DisplayName("부스를 일괄 등록하면 201과 등록 수·부스 ID 목록을 순서대로 반환한다")
    void registerBoothsInBatch() throws Exception {
        // given
        given(registerDemodayBoothUseCase.registerAll(any(RegisterDemodayBoothBatchCommand.class)))
            .willReturn(List.of(BOOTH_ID, BOOTH_ID + 1));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths/batch", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"booths": [
                      {"boothCode": 11, "projectId": 101},
                      {"boothCode": 12, "displayName": "외부 참가팀 A"}
                    ]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result.registeredCount").value(2))
            .andExpect(jsonPath("$.result.boothIds[0]").value(BOOTH_ID))
            .andExpect(jsonPath("$.result.boothIds[1]").value(BOOTH_ID + 1));

        then(registerDemodayBoothUseCase).should()
            .registerAll(new RegisterDemodayBoothBatchCommand(MEMBER_ID, POLL_ID, List.of(
                new BoothRegistration(BOOTH_CODE, PROJECT_ID, null),
                new BoothRegistration(12, null, "외부 참가팀 A"))));
    }

    @Test
    @DisplayName("일괄 등록 요청의 부스 목록이 비면 400을 반환하고 유스케이스를 호출하지 않는다")
    void rejectEmptyBatch() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths/batch", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"booths\": []}"))
            .andExpect(status().isBadRequest());

        then(registerDemodayBoothUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일괄 등록 항목의 부스 코드가 누락되면 400을 반환하고 유스케이스를 호출하지 않는다")
    void rejectBatchWhenBoothCodeIsMissing() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths/batch", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"booths\": [{\"projectId\": 101}]}"))
            .andExpect(status().isBadRequest());

        then(registerDemodayBoothUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("시스템 관리자가 아니면 일괄 등록은 권한 오류 코드를 반환한다")
    void rejectBatchWhenNotSystemAdmin() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED))
            .given(registerDemodayBoothUseCase)
            .registerAll(any(RegisterDemodayBoothBatchCommand.class));

        // when & then
        mockMvc.perform(post("/api/v1/demoday/admin/polls/{pollId}/booths/batch", POLL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"booths\": [{\"boothCode\": 11, \"projectId\": 101}]}"))
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("부스 등록 현황을 조회하면 운영 상태와 등록 개수를 함께 반환한다")
    void listBooths() throws Exception {
        // given
        DemodayAdminBoothListInfo info = new DemodayAdminBoothListInfo(
            POLL_ID,
            DemodayPollStatus.CLOSED,
            true,
            2,
            List.of(
                new DemodayBoothInfo(BOOTH_ID, BOOTH_CODE, PROJECT_ID, null),
                new DemodayBoothInfo(21L, 12, null, "외부 참가팀 A")));
        given(listDemodayAdminBoothUseCase.listBooths(POLL_ID, MEMBER_ID)).willReturn(info);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/booths", POLL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
            .andExpect(jsonPath("$.result.pollStatus").value("CLOSED"))
            .andExpect(jsonPath("$.result.boothAddable").value(true))
            .andExpect(jsonPath("$.result.boothCount").value(2))
            .andExpect(jsonPath("$.result.booths[0].boothId").value(BOOTH_ID))
            .andExpect(jsonPath("$.result.booths[0].boothCode").value(BOOTH_CODE))
            .andExpect(jsonPath("$.result.booths[0].projectId").value(PROJECT_ID))
            .andExpect(jsonPath("$.result.booths[1].boothCode").value(12))
            .andExpect(jsonPath("$.result.booths[1].displayName").value("외부 참가팀 A"));

        then(listDemodayAdminBoothUseCase).should().listBooths(POLL_ID, MEMBER_ID);
    }
}
