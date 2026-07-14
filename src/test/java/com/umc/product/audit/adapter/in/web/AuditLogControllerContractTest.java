package com.umc.product.audit.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.VARIES;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.audit.application.port.in.query.dto.SearchAuditLogQuery;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = AuditLogController.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("관리자 감사 로그 controller 계약")
class AuditLogControllerContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RestDocumentationResultHandler restDocsHandler;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetAuditLogUseCase getAuditLogUseCase;

    @Test
    @DisplayName("관리자 기존 필터를 query 객체에 전달하고 감사 로그 필드를 반환한다")
    void 관리자_기존_필터를_query_객체에_전달하고_감사_로그_필드를_반환한다() throws Exception {
        // given
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        Instant createdAt = Instant.parse("2026-01-15T12:30:00Z");
        AuditLogInfo info = new AuditLogInfo(
            1L,
            Domain.MEMBER,
            AuditAction.UPDATE,
            "MemberProfile",
            "7",
            73L,
            "회원 프로필을 수정했습니다.",
            null,
            "198.51.100.7",
            AuditOutcome.SUCCESS,
            AuditSource.ANNOTATION,
            null,
            null,
            createdAt
        );
        given(getAuditLogUseCase.search(any(SearchAuditLogQuery.class), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(info), PageRequest.of(0, 20), 1));

        // when & then
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs")
                .param("domain", "MEMBER")
                .param("action", "UPDATE")
                .param("actorMemberId", "73")
                .param("from", from.toString())
                .param("to", to.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].domain").value("MEMBER"))
            .andExpect(jsonPath("$.result.content[0].action").value("UPDATE"))
            .andExpect(jsonPath("$.result.content[0].targetType").value("MemberProfile"))
            .andExpect(jsonPath("$.result.content[0].targetId").value("7"))
            .andExpect(jsonPath("$.result.content[0].actorMemberId").value(73L))
            .andExpect(jsonPath("$.result.content[0].ipAddress").value("198.51.100.7"))
            .andExpect(jsonPath("$.result.content[0].createdAt").value(createdAt.toString()));

        ArgumentCaptor<SearchAuditLogQuery> queryCaptor = ArgumentCaptor.forClass(SearchAuditLogQuery.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(getAuditLogUseCase).should().search(queryCaptor.capture(), pageableCaptor.capture());

        assertThat(queryCaptor.getValue()).isEqualTo(
            new SearchAuditLogQuery(
                Domain.MEMBER,
                AuditAction.UPDATE,
                73L,
                from,
                to,
                null,
                null,
                null,
                null,
                null,
                null
            ));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("관리자 신규 필터를 query 객체에 전달하고 확장 응답 필드를 문서화한다")
    void 관리자_신규_필터를_query_객체에_전달하고_확장_응답_필드를_문서화한다() throws Exception {
        // given
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-28T23:59:59Z");
        Instant createdAt = Instant.parse("2026-02-10T11:12:13Z");
        AuditLogInfo jsonDetails = new AuditLogInfo(
            10L,
            Domain.SCHEDULE,
            AuditAction.UPDATE,
            "Schedule",
            "schedule-10",
            77L,
            "일정을 수정했습니다.",
            "{\"schemaVersion\":1,\"target\":{\"id\":\"schedule-10\"}}",
            "203.0.113.10",
            AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT,
            "req-2026-02-10",
            "trace-2026-02-10",
            createdAt
        );
        AuditLogInfo legacyNullDetails = new AuditLogInfo(
            11L,
            Domain.SCHEDULE,
            AuditAction.UPDATE,
            "Schedule",
            "schedule-legacy",
            77L,
            "legacy null details",
            null,
            "203.0.113.11",
            AuditOutcome.SUCCESS,
            AuditSource.ANNOTATION,
            null,
            null,
            createdAt.plusSeconds(1)
        );
        given(getAuditLogUseCase.search(any(SearchAuditLogQuery.class), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(jsonDetails, legacyNullDetails), PageRequest.of(0, 20), 2));

        // when & then
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs")
                .param("domain", "SCHEDULE")
                .param("action", "UPDATE")
                .param("actorMemberId", "77")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("targetType", "Schedule")
                .param("targetId", "schedule-10")
                .param("outcome", "FAILURE")
                .param("source", "AUTHORIZATION_ASPECT")
                .param("requestId", "req-2026-02-10")
                .param("traceId", "trace-2026-02-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].outcome").value("FAILURE"))
            .andExpect(jsonPath("$.result.content[0].source").value("AUTHORIZATION_ASPECT"))
            .andExpect(jsonPath("$.result.content[0].requestId").value("req-2026-02-10"))
            .andExpect(jsonPath("$.result.content[0].traceId").value("trace-2026-02-10"))
            .andExpect(jsonPath("$.result.content[0].details")
                .value("{\"schemaVersion\":1,\"target\":{\"id\":\"schedule-10\"}}"))
            .andExpect(jsonPath("$.result.content[1].details").doesNotExist())
            .andDo(restDocsHandler.document(
                queryParameters(
                    parameterWithName("domain").description("감사 로그 도메인 enum").optional(),
                    parameterWithName("action").description("감사 로그 액션 enum").optional(),
                    parameterWithName("actorMemberId").description("행위자 회원 ID").optional(),
                    parameterWithName("from").description("조회 시작 시각(ISO-8601 Instant, inclusive)").optional(),
                    parameterWithName("to").description("조회 종료 시각(ISO-8601 Instant, inclusive)").optional(),
                    parameterWithName("targetType").description("감사 대상 타입. 정확히 일치하는 값만 조회").optional(),
                    parameterWithName("targetId").description("감사 대상 식별자 문자열. 숫자 검증 없이 정확히 일치하는 값만 조회").optional(),
                    parameterWithName("outcome").description("감사 결과 enum: SUCCESS, FAILURE").optional(),
                    parameterWithName("source").description("감사 로그 출처 enum").optional(),
                    parameterWithName("requestId").description("요청 식별자. 정확히 일치하는 값만 조회").optional(),
                    parameterWithName("traceId").description("분산 추적 식별자. 정확히 일치하는 값만 조회").optional()
                ),
                relaxedResponseFields(
                    fieldWithPath("success").type(BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(STRING).description("응답 코드"),
                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                    fieldWithPath("result").type(OBJECT).description("페이지 응답"),
                    fieldWithPath("result.content").type(ARRAY).description("감사 로그 목록"),
                    fieldWithPath("result.content[].id").type(STRING).description("감사 로그 ID"),
                    fieldWithPath("result.content[].domain").type(STRING).description("감사 로그 도메인"),
                    fieldWithPath("result.content[].action").type(STRING).description("감사 로그 액션"),
                    fieldWithPath("result.content[].targetType").type(STRING).description("감사 대상 타입"),
                    fieldWithPath("result.content[].targetId").type(STRING).description("감사 대상 식별자 문자열"),
                    fieldWithPath("result.content[].actorMemberId").type(STRING).description("행위자 회원 ID").optional(),
                    fieldWithPath("result.content[].description").type(STRING).description("감사 로그 설명").optional(),
                    fieldWithPath("result.content[].details").type(VARIES).description("JSON 문자열 형태의 감사 스냅샷. legacy row는 null 가능").optional(),
                    fieldWithPath("result.content[].ipAddress").type(STRING).description("요청 IP").optional(),
                    fieldWithPath("result.content[].outcome").type(STRING).description("감사 결과"),
                    fieldWithPath("result.content[].source").type(STRING).description("감사 로그 출처"),
                    fieldWithPath("result.content[].requestId").type(VARIES).description("요청 식별자. legacy row는 null 가능").optional(),
                    fieldWithPath("result.content[].traceId").type(VARIES).description("분산 추적 식별자. legacy row는 null 가능").optional(),
                    fieldWithPath("result.content[].createdAt").type(STRING).description("생성 시각")
                )
            ));

        ArgumentCaptor<SearchAuditLogQuery> queryCaptor = ArgumentCaptor.forClass(SearchAuditLogQuery.class);
        then(getAuditLogUseCase).should().search(queryCaptor.capture(), any(Pageable.class));

        assertThat(queryCaptor.getValue()).isEqualTo(
            new SearchAuditLogQuery(
                Domain.SCHEDULE,
                AuditAction.UPDATE,
                77L,
                from,
                to,
                "Schedule",
                "schedule-10",
                AuditOutcome.FAILURE,
                AuditSource.AUTHORIZATION_ASPECT,
                "req-2026-02-10",
                "trace-2026-02-10"
            ));
    }

    @Test
    @DisplayName("targetId는 숫자 검증 없이 문자열 필터로 전달한다")
    void targetId는_숫자_검증_없이_문자열_필터로_전달한다() throws Exception {
        // given
        given(getAuditLogUseCase.search(any(SearchAuditLogQuery.class), any(Pageable.class)))
            .willReturn(Page.empty());

        // when & then
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs")
                .param("targetId", "uuid-or-compound-id"))
            .andExpect(status().isOk());

        ArgumentCaptor<SearchAuditLogQuery> queryCaptor = ArgumentCaptor.forClass(SearchAuditLogQuery.class);
        then(getAuditLogUseCase).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue().targetId()).isEqualTo("uuid-or-compound-id");
    }
}
