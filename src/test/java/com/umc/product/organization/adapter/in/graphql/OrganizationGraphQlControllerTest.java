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

import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
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
                query ($ids: [ID!]!) {
                  gisus(filter: { ids: $ids }) {
                    edges {
                      node {
                        id
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
                }
                """)
            .variable("ids", List.of(
                GlobalId.encode(GlobalIdTypes.GISU, 1L),
                GlobalId.encode(GlobalIdTypes.GISU, 1L),
                GlobalId.encode(GlobalIdTypes.GISU, 2L)
            ))
            .execute()
            .path("gisus.edges[0].node.id").entity(String.class)
            .isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 1L))
            .path("gisus.edges[0].node.generation").entity(Integer.class).isEqualTo(10)
            .path("gisus.edges[0].node.active").entity(Boolean.class).isEqualTo(true)
            .path("gisus.edges[0].node.chapters[0].chapterName").entity(String.class).isEqualTo("Ain 지부")
            .path("gisus.edges[0].node.chapters[0].schools[0].schoolName").entity(String.class)
            .isEqualTo("중앙대학교")
            .path("gisus.edges[0].node.schools[0].links[0].type").entity(String.class)
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
                  gisus(filter: { generations: [11, 11, 12] }) {
                    edges {
                      node {
                        id
                        generation
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("gisus.edges[0].node.generation").entity(Integer.class).isEqualTo(11);

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
                  gisus(filter: { active: true }) {
                    edges { node { active } }
                  }
                }
                """)
            .execute()
            .path("gisus.edges[0].node.active").entity(Boolean.class).isEqualTo(true);

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
                  gisus(filter: {}) { totalCount }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisus"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기수 조직 조회 기준을 둘 이상 보내면 GraphQL error를 반환한다")
    void 기수_조직_조회_기준을_둘_이상_보내면_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query ($ids: [ID!]) {
                  gisus(filter: { ids: $ids, generations: [10] }) { totalCount }
                }
                """)
            .variable("ids", List.of(GlobalId.encode(GlobalIdTypes.GISU, 1L)))
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisus"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("active false는 GraphQL error를 반환한다")
    void active_false는_GraphQL_error를_반환한다() {
        graphQlTester.document("""
                query {
                  gisus(filter: { active: false }) { totalCount }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertOrganizationBadRequest(errors, "gisus"));

        then(getGisuOrganizationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("includeChapters와 includeSchools는 GraphQL 입력으로 허용하지 않는다")
    void includeChapters와_includeSchools는_GraphQL_입력으로_허용하지_않는다() {
        graphQlTester.document("""
                query {
                  gisus(filter: {
                    ids: ["R2lzdTox"]
                    includeChapters: true
                    includeSchools: true
                  }) { totalCount }
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
                query ($id: ID!) {
                  gisu(id: $id) {
                    id
                    generation
                    active
                  }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.GISU, 1L))
            .execute()
            .path("gisu.id").entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 1L))
            .path("gisu.generation").entity(Integer.class).isEqualTo(10)
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
                    id
                    generation
                    active
                  }
                }
                """)
            .execute()
            .path("activeGisu.id").entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 3L))
            .path("activeGisu.generation").entity(Integer.class).isEqualTo(12)
            .path("activeGisu.active").entity(Boolean.class).isEqualTo(true);

        then(getGisuUseCase).should().getActiveGisu();
    }

    @Test
    @DisplayName("전체 지부 조회와 지부 단건 조회는 GetChapterUseCase를 호출한다")
    void 전체_지부_조회와_지부_단건_조회는_GetChapterUseCase를_호출한다() {
        given(getChapterUseCase.getAllChapters()).willReturn(List.of(new ChapterInfo(1L, "Ain 지부")));
        given(getChapterUseCase.getChapterById(2L)).willReturn(new ChapterInfo(2L, "Ner 지부"));

        graphQlTester.document("""
                query ($id: ID!) {
                  chapters {
                    edges { node { id name } }
                  }
                  chapter(id: $id) {
                    id
                    name
                  }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.CHAPTER, 2L))
            .execute()
            .path("chapters.edges[0].node.id").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.CHAPTER, 1L))
            .path("chapters.edges[0].node.name").entity(String.class).isEqualTo("Ain 지부")
            .path("chapter.id").entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.CHAPTER, 2L))
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
                query ($id: ID!) {
                  schools {
                    edges { node { schoolId schoolName } }
                  }
                  school(id: $id) {
                    id
                    schoolName
                    active
                    createdAt
                    updatedAt
                  }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.SCHOOL, 2L))
            .execute()
            .path("schools.edges[0].node.schoolId").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.SCHOOL, 1L))
            .path("schools.edges[0].node.schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("school.id").entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.SCHOOL, 2L))
            .path("school.schoolName").entity(String.class).isEqualTo("동국대학교")
            .path("school.active").entity(Boolean.class).isEqualTo(true);

        then(getSchoolUseCase).should().getAllSchoolNames();
        then(getSchoolUseCase).should().getSchoolDetail(2L);
    }

    private void assertOrganizationBadRequest(
        List<org.springframework.graphql.ResponseError> errors,
        String path
    ) {
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getPath()).isEqualTo(path);
            assertThat(error.getExtensions())
                .containsEntry("code", OrganizationErrorCode.GISU_QUERY_CONDITION_INVALID.getCode());
        });
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
