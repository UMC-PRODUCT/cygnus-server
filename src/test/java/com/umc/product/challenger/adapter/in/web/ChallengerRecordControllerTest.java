package com.umc.product.challenger.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.umc.product.challenger.adapter.in.web.assembler.ChallengerRecordResponseAssembler;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
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
