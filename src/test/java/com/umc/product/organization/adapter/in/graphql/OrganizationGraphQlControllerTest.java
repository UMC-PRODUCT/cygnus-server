package com.umc.product.organization.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationQuery;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.organization.exception.OrganizationErrorCode;

@GraphQlTest(OrganizationGraphQlController.class)
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class})
@DisplayName("OrganizationGraphQlController")
class OrganizationGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetGisuOrganizationUseCase getGisuOrganizationUseCase;

    @MockitoBean
    GetGisuUseCase getGisuUseCase;

    @MockitoBean
    GetChapterUseCase getChapterUseCase;

    @MockitoBean
    GetSchoolUseCase getSchoolUseCase;

    @Test
    void 기수조회는_트랙_학습방식을_노출한다() {
        // given
        GisuInfo info = new GisuInfo(11L, 11L, Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2027-03-01T00:00:00Z"), false, GisuLearningType.TRACK);
        given(getGisuOrganizationUseCase.get(any()))
            .willReturn(List.of(GisuOrganizationInfo.of(info, List.of(), List.of())));

        // when & then
        graphQlTester.document("""
                query {
                  gisuOrganizations(input: { ids: [11] }) {
                    gisus { gisuId learningType }
                  }
                }
                """)
            .execute()
            .path("gisuOrganizations.gisus[0].learningType").entity(String.class).isEqualTo("TRACK");
    }

    @Test
    @DisplayName("기수 조직 조회의 하위 지부와 학교는 GraphQL field resolver가 batch로 조회한다")
    void 기수_조직_조회의_하위_지부와_학교는_GraphQL_field_resolver가_batch로_조회한다() {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(gisu(1L, 10L), gisu(2L, 11L)));
        given(getChapterUseCase.listByGisuIds(Set.of(1L, 2L))).willReturn(Map.of(
            1L, List.of(new ChapterInfo(100L, "Ain 지부")),
            2L, List.of(new ChapterInfo(200L, "Ner 지부"))
        ));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuIds(Set.of(1L, 2L))).willReturn(Map.of(
            1L, List.of(chapterWithSchools(100L, "Ain 지부", 1000L, "중앙대학교")),
            2L, List.of(chapterWithSchools(200L, "Ner 지부", 2000L, "동국대학교"))
        ));
        given(getSchoolUseCase.getSchoolListByGisuIds(Set.of(1L, 2L))).willReturn(Map.of(
            1L, List.of(schoolDetail(1000L, "중앙대학교")),
            2L, List.of(schoolDetail(2000L, "동국대학교"))
        ));

        graphQlTester.document("""
                query {
                  gisuOrganizations(input: {
                    ids: [1, 1, 2]
                  }) {
                    gisus {
                      gisuId
                      generation
                      active
                      startAt
                      chapters {
                        chapterId
                        chapterName
                        schools {
                          schoolId
                          schoolName
                        }
                      }
                      schools {
                        schoolId
                        schoolName
                        active
                        links {
                          title
                          type
                          url
                        }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("gisuOrganizations.gisus[0].gisuId").entity(String.class).isEqualTo("1")
            .path("gisuOrganizations.gisus[0].generation").entity(String.class).isEqualTo("10")
            .path("gisuOrganizations.gisus[0].active").entity(Boolean.class).isEqualTo(true)
            .path("gisuOrganizations.gisus[0].chapters[0].chapterName").entity(String.class).isEqualTo("Ain 지부")
            .path("gisuOrganizations.gisus[0].chapters[0].schools[0].schoolName").entity(String.class)
            .isEqualTo("중앙대학교")
            .path("gisuOrganizations.gisus[0].schools[0].links[0].type").entity(String.class)
            .isEqualTo("KAKAO");

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        GisuOrganizationQuery query = captor.getValue();
        assertThat(query.selector()).isEqualTo(GisuOrganizationQuery.Selector.ID);
        assertThat(query.ids()).containsExactly(1L, 2L);
        assertThat(query.generations()).isEmpty();
        assertThat(query.includeChapter()).isFalse();
        assertThat(query.includeSchool()).isFalse();
        then(getChapterUseCase).should().listByGisuIds(Set.of(1L, 2L));
        then(getChapterUseCase).should().getChaptersWithSchoolsByGisuIds(Set.of(1L, 2L));
        then(getSchoolUseCase).should().getSchoolListByGisuIds(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("기수 조직 조회는 generation 중복을 제거한다")
    void 기수_조직_조회는_generation_중복을_제거한다() {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(gisu(2L, 11L)));

        graphQlTester.document("""
                query {
                  gisuOrganizations(input: { generations: [11, 11, 12] }) {
                    gisus {
                      gisuId
                      generation
                    }
                  }
                }
                """)
            .execute()
            .path("gisuOrganizations.gisus[0].generation").entity(String.class).isEqualTo("11");

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        assertThat(captor.getValue().selector()).isEqualTo(GisuOrganizationQuery.Selector.GENERATION);
        assertThat(captor.getValue().generations()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("기수 조직 조회는 active true selector를 전달한다")
    void 기수_조직_조회는_active_true_selector를_전달한다() {
        given(getGisuOrganizationUseCase.get(any())).willReturn(List.of(gisu(3L, 12L)));

        graphQlTester.document("""
                query {
                  gisuOrganizations(input: { active: true }) {
                    gisus {
                      active
                    }
                  }
                }
                """)
            .execute()
            .path("gisuOrganizations.gisus[0].active").entity(Boolean.class).isEqualTo(true);

        ArgumentCaptor<GisuOrganizationQuery> captor = ArgumentCaptor.forClass(GisuOrganizationQuery.class);
        then(getGisuOrganizationUseCase).should().get(captor.capture());
        assertThat(captor.getValue().selector()).isEqualTo(GisuOrganizationQuery.Selector.ACTIVE);
        assertThat(captor.getValue().active()).isTrue();
    }

    @Test
    @DisplayName("기수 조직 조회 기준이 없으면 GraphQL error를 반환한다")
    void 기수_조직_조회_기준이_없으면_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query {
                  gisuOrganizations(input: {}) {
                    gisus {
                      gisuId
                    }
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisuOrganizations"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기수 조직 조회 기준을 둘 이상 보내면 GraphQL error를 반환한다")
    void 기수_조직_조회_기준을_둘_이상_보내면_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query {
                  gisuOrganizations(input: { ids: [1], generations: [10] }) {
                    gisus {
                      gisuId
                    }
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisuOrganizations"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("active false는 GraphQL error를 반환한다")
    void active_false는_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query {
                  gisuOrganizations(input: { active: false }) {
                    gisus {
                      gisuId
                    }
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisuOrganizations"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("includeChapters와 includeSchools는 GraphQL 입력으로 허용하지 않는다")
    void includeChapters와_includeSchools는_GraphQL_입력으로_허용하지_않는다() {
        graphQlTester.document("""
                query {
                  gisuOrganizations(input: {
                    ids: [1]
                    includeChapters: true
                    includeSchools: true
                  }) {
                    gisus {
                      gisuId
                    }
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기수 단건 조회는 GetGisuUseCase를 호출한다")
    void 기수_단건_조회는_GetGisuUseCase를_호출한다() {
        given(getGisuUseCase.getById(1L)).willReturn(gisuInfo(1L, 10L));

        graphQlTester.document("""
                query {
                  gisu(id: 1) {
                    gisuId
                    generation
                    active
                  }
                }
                """)
            .execute()
            .path("gisu.gisuId").entity(String.class).isEqualTo("1")
            .path("gisu.generation").entity(String.class).isEqualTo("10")
            .path("gisu.active").entity(Boolean.class).isEqualTo(true);

        then(getGisuUseCase).should().getById(1L);
    }

    @Test
    @DisplayName("활성 기수 조회는 GetGisuUseCase를 호출한다")
    void 활성_기수_조회는_GetGisuUseCase를_호출한다() {
        given(getGisuUseCase.getActiveGisu()).willReturn(gisuInfo(3L, 12L));

        graphQlTester.document("""
                query {
                  activeGisu {
                    gisuId
                    generation
                    active
                  }
                }
                """)
            .execute()
            .path("activeGisu.gisuId").entity(String.class).isEqualTo("3")
            .path("activeGisu.generation").entity(String.class).isEqualTo("12")
            .path("activeGisu.active").entity(Boolean.class).isEqualTo(true);

        then(getGisuUseCase).should().getActiveGisu();
    }

    @Test
    @DisplayName("전체 지부 조회와 지부 단건 조회는 GetChapterUseCase를 호출한다")
    void 전체_지부_조회와_지부_단건_조회는_GetChapterUseCase를_호출한다() {
        given(getChapterUseCase.getAllChapters()).willReturn(List.of(new ChapterInfo(1L, "Ain 지부")));
        given(getChapterUseCase.getChapterById(2L)).willReturn(new ChapterInfo(2L, "Ner 지부"));

        graphQlTester.document("""
                query {
                  chapters {
                    id
                    name
                  }
                  chapter(id: 2) {
                    id
                    name
                  }
                }
                """)
            .execute()
            .path("chapters[0].id").entity(String.class).isEqualTo("1")
            .path("chapters[0].name").entity(String.class).isEqualTo("Ain 지부")
            .path("chapter.id").entity(String.class).isEqualTo("2")
            .path("chapter.name").entity(String.class).isEqualTo("Ner 지부");

        then(getChapterUseCase).should().getAllChapters();
        then(getChapterUseCase).should().getChapterById(2L);
    }

    @Test
    @DisplayName("전체 학교 조회와 학교 단건 조회는 GetSchoolUseCase를 호출한다")
    void 전체_학교_조회와_학교_단건_조회는_GetSchoolUseCase를_호출한다() {
        given(getSchoolUseCase.getAllSchoolNames()).willReturn(List.of(new SchoolNameInfo(1L, "중앙대학교")));
        given(getSchoolUseCase.getSchoolDetail(2L)).willReturn(schoolDetail(2L, "동국대학교"));

        graphQlTester.document("""
                query {
                  schools {
                    schoolId
                    schoolName
                  }
                  school(id: 2) {
                    schoolId
                    schoolName
                    active
                    createdAt
                    updatedAt
                  }
                }
                """)
            .execute()
            .path("schools[0].schoolId").entity(String.class).isEqualTo("1")
            .path("schools[0].schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("school.schoolId").entity(String.class).isEqualTo("2")
            .path("school.schoolName").entity(String.class).isEqualTo("동국대학교")
            .path("school.active").entity(Boolean.class).isEqualTo(true);

        then(getSchoolUseCase).should().getAllSchoolNames();
        then(getSchoolUseCase).should().getSchoolDetail(2L);
    }

    private void assertOrganizationBadRequest(
        List<org.springframework.graphql.ResponseError> errors,
        String path
    ) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getPath()).isEqualTo(path);
        assertThat(errors.get(0).getExtensions())
            .containsEntry("code", OrganizationErrorCode.GISU_QUERY_CONDITION_INVALID.getCode());
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

    private GisuInfo gisuInfo(Long gisuId, Long generation) {
        return new GisuInfo(
            gisuId,
            generation,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T23:59:59Z"),
            true
        );
    }

    private ChapterWithSchoolsInfo chapterWithSchools(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName
    ) {
        return new ChapterWithSchoolsInfo(
            chapterId,
            chapterName,
            List.of(new ChapterWithSchoolsInfo.SchoolInfo(schoolId, schoolName))
        );
    }

    private SchoolDetailInfo schoolDetail(Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            10L,
            "Ain 지부",
            schoolName,
            null,
            schoolId,
            "비고",
            "https://storage.example.com/school-logo.png",
            List.of(new SchoolDetailInfo.SchoolLinkItem(
                "카카오톡",
                SchoolLinkType.KAKAO,
                "https://open.kakao.com/example"
            )),
            true,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-02T00:00:00Z")
        );
    }
}
