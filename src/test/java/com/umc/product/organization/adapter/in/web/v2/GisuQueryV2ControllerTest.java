package com.umc.product.organization.adapter.in.web.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(controllers = GisuQueryV2Controller.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GisuQueryV2Controller")
class GisuQueryV2ControllerTest {

    @Autowired
    MockMvc mockMvc;

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
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.result.gisus").isArray())
            .andExpect(jsonPath("$.result.gisus.length()").value(2))
            .andExpect(jsonPath("$.result.gisus[0].gisuId").isString())
            .andExpect(jsonPath("$.result.gisus[0].gisuId").value(1L))
            .andExpect(jsonPath("$.result.gisus[0].generation").isString())
            .andExpect(jsonPath("$.result.gisus[0].generation").value(9L))
            .andExpect(jsonPath("$.result.gisus[0].gisu").doesNotExist())
            .andExpect(jsonPath("$.result.gisus[0].startAt").value("2026-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.result.gisus[0].endAt").value("2026-08-31T23:59:59Z"))
            .andExpect(jsonPath("$.result.gisus[0].isActive").value(true))
            .andExpect(jsonPath("$.result.gisus[0].chapters").isArray())
            .andExpect(jsonPath("$.result.gisus[0].chapters.length()").value(1))
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].chapterId").isString())
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].chapterId").value("100"))
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].chapterName").value("Ain 지부"))
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].schools").isArray())
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].schools[0].schoolId").value("1000"))
            .andExpect(jsonPath("$.result.gisus[0].chapters[0].schools[0].schoolName").value("중앙대학교"))
            .andExpect(jsonPath("$.result.gisus[0].schools").isArray())
            .andExpect(jsonPath("$.result.gisus[0].schools.length()").value(1))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].chapterId").value("100"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].chapterName").value("Ain 지부"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].schoolId").value("1000"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].schoolName").value("중앙대학교"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].shortName").doesNotExist())
            .andExpect(jsonPath("$.result.gisus[0].schools[0].remark").value("비고"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].logoImageUrl")
                .value("https://storage.example.com/school-logo.png"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].links").isArray())
            .andExpect(jsonPath("$.result.gisus[0].schools[0].links[0].title").value("인스타그램"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].links[0].type").value("INSTAGRAM"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].links[0].url")
                .value("https://instagram.com/example"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].isActive").value(true))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].createdAt").value("2026-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.result.gisus[0].schools[0].updatedAt").value("2026-03-02T00:00:00Z"))
            .andExpect(jsonPath("$.result.gisus[1].gisuId").value("2"))
            .andExpect(jsonPath("$.result.gisus[1].chapters").isEmpty())
            .andExpect(jsonPath("$.result.gisus[1].schools").isEmpty());

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
