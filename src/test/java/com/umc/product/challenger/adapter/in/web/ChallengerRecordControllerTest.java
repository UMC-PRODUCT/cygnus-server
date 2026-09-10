package com.umc.product.challenger.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = ChallengerRecordController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChallengerRecordController")
class ChallengerRecordControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ChallengerRecordResponseAssembler assembler;

    @MockitoBean
    ManageChallengerRecordUseCase manageChallengerRecordUseCase;

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
    @DisplayName("코드로 챌린저 기록을 조회한다")
    void 코드로_챌린저_기록을_조회한다() throws Exception {
        given(assembler.from("ABC123")).willReturn(ChallengerRecordResponse.builder()
            .code("ABC123")
            .part(ChallengerPart.SPRINGBOOT)
            .build());

        mockMvc.perform(get("/api/v1/challenger-record/code/{code}", "ABC123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.code").value("ABC123"));
    }

    @Test
    @DisplayName("기본 트랙 코드 발급 요청을 전달하고 응답에 트랙을 노출한다")
    void 기본_트랙_코드_발급_요청을_전달하고_응답에_트랙을_노출한다() throws Exception {
        // given
        given(manageChallengerRecordUseCase.create(any())).willReturn(10L);
        given(assembler.from(10L)).willReturn(ChallengerRecordResponse.builder()
            .code("ABC123").track(ChallengerTrack.WEB_PRODUCT_ENGINEER).build());

        // when & then
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gisuId":1,"chapterId":2,"schoolId":3,"track":"WEB_PRODUCT_ENGINEER","memberName":"홍길동"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.track").value("WEB_PRODUCT_ENGINEER"))
            .andExpect(jsonPath("$.result.tracks[0]").value("WEB_PRODUCT_ENGINEER"));

        ArgumentCaptor<CreateChallengerRecordCommand> captor =
            ArgumentCaptor.forClass(CreateChallengerRecordCommand.class);
        then(manageChallengerRecordUseCase).should().create(captor.capture());
        assertThat(captor.getValue().part()).isNull();
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getValue().tracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getValue().creatorMemberId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("복수 트랙과 담당 역할을 단건 발급하고 단일 track 응답은 비운다")
    void 복수_트랙과_담당_역할을_단건_발급한다() throws Exception {
        // Given
        given(manageChallengerRecordUseCase.create(any())).willReturn(10L);
        given(assembler.from(10L)).willReturn(ChallengerRecordResponse.builder()
            .code("ABC123").part(ChallengerPart.SPRINGBOOT)
            .tracks(List.of(ChallengerTrack.DESIGN, ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .challengerRoleType(ChallengerRoleType.SCHOOL_PART_LEADER).build());

        // When / Then
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gisuId":1,"chapterId":2,"schoolId":3,"part":"SPRINGBOOT",
                     "tracks":["DESIGN","WEB_PRODUCT_ENGINEER"],"memberName":"홍길동",
                     "challengerRoleType":"SCHOOL_PART_LEADER"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.track").isEmpty())
            .andExpect(jsonPath("$.result.tracks[0]").value("DESIGN"))
            .andExpect(jsonPath("$.result.tracks[1]").value("WEB_PRODUCT_ENGINEER"));
        ArgumentCaptor<CreateChallengerRecordCommand> captor =
            ArgumentCaptor.forClass(CreateChallengerRecordCommand.class);
        then(manageChallengerRecordUseCase).should().create(captor.capture());
        assertThat(captor.getValue().tracks()).containsExactly(ChallengerTrack.DESIGN, ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getValue().part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(captor.getValue().challengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PART_LEADER);
    }

    @Test
    @DisplayName("일괄 발급에서 복수 트랙과 지부 없는 비수강 중앙 운영진을 전달한다")
    void 일괄_발급에서_복수_트랙과_지부_없는_중앙_운영진을_전달한다() throws Exception {
        // Given
        given(manageChallengerRecordUseCase.createBulk(any())).willReturn(List.of(10L, 11L));

        // When / Then
        mockMvc.perform(post("/api/v1/challenger-record/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"gisuId":1,"chapterId":2,"schoolId":3,"tracks":["PLAN","DESIGN"],"memberName":"홍길동"},
                     {"gisuId":1,"schoolId":3,"tracks":[],"memberName":"김철수",
                      "challengerRoleType":"CENTRAL_OPERATING_TEAM_MEMBER"}]
                    """))
            .andExpect(status().isOk());
        ArgumentCaptor<List<CreateChallengerRecordCommand>> captor = ArgumentCaptor.captor();
        then(manageChallengerRecordUseCase).should().createBulk(captor.capture());
        assertThat(captor.getValue().getFirst().tracks()).containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
        assertThat(captor.getValue().getLast().tracks()).isEmpty();
        assertThat(captor.getValue().getLast().chapterId()).isNull();
        assertThat(captor.getValue().getLast().toEntity().canOmitChapter()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"tracks\":[],",
        "\"tracks\":[],\"challengerRoleType\":\"SCHOOL_PRESIDENT\",",
        "\"tracks\":[\"PLAN\"],\"challengerRoleType\":\"CENTRAL_PRESIDENT\","
    })
    @DisplayName("비수강 중앙 운영진 외에는 지부 없는 코드 발급을 거부한다")
    void 비수강_중앙_운영진_외에는_지부_생략을_거부한다(String selection) throws Exception {
        // Given / When / Then
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gisuId":1,"schoolId":3,%s"memberName":"홍길동"}
                    """.formatted(selection)))
            .andExpect(status().isBadRequest());
        then(manageChallengerRecordUseCase).should(never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"part\":\"WEB\",\"track\":\"WEB_PRODUCT_ENGINEER\",",
        "\"track\":\"INFRA_PLUS\",",
        "\"tracks\":[\"INFRA_PLUS\"],\"challengerRoleType\":\"SCHOOL_PART_LEADER\",",
        "\"part\":\"WEB\",\"tracks\":[\"WEB_PRODUCT_ENGINEER\"],",
        "\"track\":\"DESIGN\",\"tracks\":[],",
        "\"tracks\":[null],",
        "\"tracks\":[],",
        ""
    })
    @DisplayName("혼합 유형과 PLUS 및 빈 학습 유형은 코드로 발급할 수 없다")
    void 혼합_유형과_PLUS_및_빈_학습_유형은_코드로_발급할_수_없다(String selection) throws Exception {
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gisuId":1,"chapterId":2,"schoolId":3,%s"memberName":"홍길동"}
                    """.formatted(selection)))
            .andExpect(status().isBadRequest());

        then(manageChallengerRecordUseCase).should(never()).create(any());
    }

    @Test
    @DisplayName("회원 기록 추가 요청의 code가 blank이면 400")
    void 회원_기록_추가_요청의_code가_blank이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/challenger-record/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":" "}
                    """))
            .andExpect(status().isBadRequest());

        then(manageChallengerRecordUseCase).should(never()).consumeCode(any(ConsumeChallengerRecordCommand.class));
    }

    @Test
    @DisplayName("기록 생성 요청의 gisuId가 없으면 400")
    void 기록_생성_요청의_gisuId가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"chapterId":2,"schoolId":3,"part":"SPRINGBOOT","memberName":"홍길동"}
                    """))
            .andExpect(status().isBadRequest());

        then(manageChallengerRecordUseCase).should(never()).create(any());
    }

    @Test
    @DisplayName("기록 bulk 생성 요청 내부 항목의 gisuId가 없으면 400")
    void 기록_bulk_생성_요청_내부_항목의_gisuId가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/challenger-record/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"chapterId":2,"schoolId":3,"part":"SPRINGBOOT","memberName":"홍길동"}]
                    """))
            .andExpect(status().isBadRequest());

        then(manageChallengerRecordUseCase).should(never()).createBulk(any());
    }

    @Test
    @DisplayName("챌린저 기록 생성 요청에 SUPER_ADMIN을 입력하면 400")
    void 챌린저_기록_생성_요청에_super_admin을_입력하면_400() throws Exception {
        mockMvc.perform(post("/api/v1/challenger-record")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gisuId": 1,
                      "chapterId": 2,
                      "schoolId": 3,
                      "part": "SPRINGBOOT",
                      "memberName": "홍길동",
                      "challengerRoleType": "SUPER_ADMIN"
                    }
                    """))
            .andExpect(status().isBadRequest());

        then(manageChallengerRecordUseCase).should(never()).create(any());
    }
}
