package com.umc.product.organization.adapter.in.web.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo.ChapterOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo.ChapterSchoolInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo.SchoolLinkInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo.SchoolOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationQuery;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = GisuQueryV2Controller.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("GisuQueryV2Controller")
class GisuQueryV2ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RestDocumentationResultHandler restDocsHandler;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetGisuOrganizationUseCase getGisuOrganizationUseCase;

    @Test
    @DisplayName("id 목록은 중복을 제거하고 첫 등장 순서대로 조회한다")
    void id_목록은_중복을_제거하고_첫_등장_순서대로_조회한다() throws Exception {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(
            gisuWithOrganizations(1L, 9L),
            gisu(2L, 10L)
        ));

        mockMvc.perform(get("/api/v2/gisu")
                .param("id", "1", "1", "2")
                .param("includeChapter", "true")
                .param("includeSchool", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.gisus.length()").value(2))
            .andExpect(jsonPath("$.result.gisus[0].gisuId").value(1L))
            .andExpect(jsonPath("$.result.gisus[0].generation").value(9L))
            .andExpect(jsonPath("$.result.gisus[0].gisu").doesNotExist())
            .andExpect(jsonPath("$.result.gisus[0].chapters").isArray())
            .andExpect(jsonPath("$.result.gisus[0].schools").isArray())
            .andDo(restDocsHandler.document(
                queryParameters(
                    parameterWithName("id").description("기수 ID 목록. 중복 값은 첫 등장 순서를 유지하며 제거").optional(),
                    parameterWithName("generation").description("기수 번호 목록. id/active와 동시에 사용할 수 없음").optional(),
                    parameterWithName("active").description("활성 기수 조회 여부. true만 유효").optional(),
                    parameterWithName("includeChapter").description("기수 내 지부 정보 포함 여부. 기본 false").optional(),
                    parameterWithName("includeSchool").description("기수 내 학교 정보 포함 여부. 기본 false").optional()
                ),
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                    fieldWithPath("result.gisus").type(JsonFieldType.ARRAY).description("기수 조직 목록"),
                    fieldWithPath("result.gisus[].gisuId").type(JsonFieldType.STRING).description("기수 ID"),
                    fieldWithPath("result.gisus[].generation").type(JsonFieldType.STRING).description("기수 번호"),
                    fieldWithPath("result.gisus[].startAt").type(JsonFieldType.STRING).description("기수 시작 일시"),
                    fieldWithPath("result.gisus[].endAt").type(JsonFieldType.STRING).description("기수 종료 일시"),
                    fieldWithPath("result.gisus[].isActive").type(JsonFieldType.BOOLEAN).description("활성 기수 여부"),
                    fieldWithPath("result.gisus[].chapters").type(JsonFieldType.ARRAY)
                        .description("기수 내 지부 목록. includeChapter=false이면 빈 배열"),
                    fieldWithPath("result.gisus[].chapters[].chapterId").type(JsonFieldType.STRING).description("지부 ID"),
                    fieldWithPath("result.gisus[].chapters[].chapterName").type(JsonFieldType.STRING).description("지부 이름"),
                    fieldWithPath("result.gisus[].chapters[].schools").type(JsonFieldType.ARRAY)
                        .description("지부 내 학교 목록. includeSchool=false이면 빈 배열"),
                    fieldWithPath("result.gisus[].chapters[].schools[].schoolId").type(JsonFieldType.STRING)
                        .description("지부 내 학교 ID"),
                    fieldWithPath("result.gisus[].chapters[].schools[].schoolName").type(JsonFieldType.STRING)
                        .description("지부 내 학교 이름"),
                    fieldWithPath("result.gisus[].schools").type(JsonFieldType.ARRAY)
                        .description("기수 내 학교 목록. includeSchool=false이면 빈 배열"),
                    fieldWithPath("result.gisus[].schools[].chapterId").type(JsonFieldType.STRING).description("학교 소속 지부 ID"),
                    fieldWithPath("result.gisus[].schools[].chapterName").type(JsonFieldType.STRING).description("학교 소속 지부 이름"),
                    fieldWithPath("result.gisus[].schools[].schoolId").type(JsonFieldType.STRING).description("학교 ID"),
                    fieldWithPath("result.gisus[].schools[].schoolName").type(JsonFieldType.STRING).description("학교 이름"),
                    fieldWithPath("result.gisus[].schools[].shortName").type(JsonFieldType.STRING).description("학교 약칭").optional(),
                    fieldWithPath("result.gisus[].schools[].remark").type(JsonFieldType.STRING).description("학교 비고"),
                    fieldWithPath("result.gisus[].schools[].logoImageUrl").type(JsonFieldType.STRING).description("학교 로고 이미지 URL"),
                    fieldWithPath("result.gisus[].schools[].links").type(JsonFieldType.ARRAY).description("학교 링크 목록"),
                    fieldWithPath("result.gisus[].schools[].links[].title").type(JsonFieldType.STRING).description("링크 제목"),
                    fieldWithPath("result.gisus[].schools[].links[].type").type(JsonFieldType.STRING).description("링크 타입"),
                    fieldWithPath("result.gisus[].schools[].links[].url").type(JsonFieldType.STRING).description("링크 URL"),
                    fieldWithPath("result.gisus[].schools[].isActive").type(JsonFieldType.BOOLEAN).description("학교 활성 여부"),
                    fieldWithPath("result.gisus[].schools[].createdAt").type(JsonFieldType.STRING).description("학교 생성 일시"),
                    fieldWithPath("result.gisus[].schools[].updatedAt").type(JsonFieldType.STRING).description("학교 수정 일시")
                )
            ));

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        GisuOrganizationQuery query = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(query.ids()).containsExactly(1L, 2L);
        org.assertj.core.api.Assertions.assertThat(query.generations()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(query.includeChapter()).isTrue();
        org.assertj.core.api.Assertions.assertThat(query.includeSchool()).isTrue();
    }

    @Test
    @DisplayName("generation 목록은 중복을 제거하고 조회한다")
    void generation_목록은_중복을_제거하고_조회한다() throws Exception {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(
            gisu(7L, 9L),
            gisu(8L, 10L)
        ));

        mockMvc.perform(get("/api/v2/gisu")
                .param("generation", "9", "9", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.gisus.length()").value(2))
            .andExpect(jsonPath("$.result.gisus[0].generation").value(9L))
            .andExpect(jsonPath("$.result.gisus[0].chapters.length()").value(0))
            .andExpect(jsonPath("$.result.gisus[0].schools.length()").value(0));

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().generations()).containsExactly(9L, 10L);
    }

    @Test
    @DisplayName("active=true는 활성 기수만 조회한다")
    void active_true는_활성_기수만_조회한다() throws Exception {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(gisu(10L, 11L)));

        mockMvc.perform(get("/api/v2/gisu")
                .param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.gisus.length()").value(1))
            .andExpect(jsonPath("$.result.gisus[0].gisuId").value(10L));

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().active()).isTrue();
    }

    @Test
    @DisplayName("조회 기준이 없으면 400을 반환한다")
    void 조회_기준이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v2/gisu"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORGANIZATION-0065"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회 기준을 둘 이상 보내면 400을 반환한다")
    void 조회_기준을_둘_이상_보내면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v2/gisu")
                .param("id", "1")
                .param("generation", "9"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORGANIZATION-0065"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("active=false는 400을 반환한다")
    void active_false는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v2/gisu")
                .param("active", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ORGANIZATION-0065"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    private GisuOrganizationInfo gisu(Long gisuId, Long generation) {
        return new GisuOrganizationInfo(
            gisuId,
            generation,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T23:59:59Z"),
            true,
            List.of(),
            List.of()
        );
    }

    private GisuOrganizationInfo gisuWithOrganizations(Long gisuId, Long generation) {
        Instant createdAt = Instant.parse("2026-03-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-02T00:00:00Z");
        return new GisuOrganizationInfo(
            gisuId,
            generation,
            createdAt,
            Instant.parse("2026-08-31T23:59:59Z"),
            true,
            List.of(new ChapterOrganizationInfo(
                100L,
                "Ain 지부",
                List.of(new ChapterSchoolInfo(1000L, "중앙대학교"))
            )),
            List.of(new SchoolOrganizationInfo(
                100L,
                "Ain 지부",
                1000L,
                "중앙대학교",
                null,
                "비고",
                "https://storage.example.com/school-logo.png",
                List.of(new SchoolLinkInfo("인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/example")),
                true,
                createdAt,
                updatedAt
            ))
        );
    }
}
