package com.umc.product.organization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.umc.product.organization.adapter.out.persistence.chapter.ChapterSchoolQueryRepository;
import com.umc.product.organization.adapter.out.persistence.gisu.GisuQueryRepository;
import com.umc.product.organization.adapter.out.persistence.school.SchoolQueryRepository;
import com.umc.product.organization.adapter.out.persistence.umcproduct.UmcProductMemberQueryRepository;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolSearchCondition;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.SchoolLink;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    SchoolQueryRepository.class,
    ChapterSchoolQueryRepository.class,
    GisuQueryRepository.class,
    UmcProductMemberQueryRepository.class
})
@DisplayName("Organization QueryDSL 저장소 통합")
class OrganizationQueryRepositoryIntegrationTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-12-31T00:00:00Z");
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Autowired
    TestEntityManager em;

    @Autowired
    SchoolQueryRepository schoolQueryRepository;

    @Autowired
    ChapterSchoolQueryRepository chapterSchoolQueryRepository;

    @Autowired
    GisuQueryRepository gisuQueryRepository;

    @Autowired
    UmcProductMemberQueryRepository memberQueryRepository;

    Gisu activeGisu;
    Gisu inactiveGisu;
    Chapter activeChapter;
    Chapter inactiveChapter;
    School alphaSchool;
    School betaSchool;
    ChapterSchool activeAssignment;
    ChapterSchool inactiveAssignment;

    @BeforeEach
    void setUpSchoolGraph() {
        activeGisu = em.getEntityManager()
            .createQuery("select g from Gisu g where g.isActive = true", Gisu.class)
            .getSingleResult();
        inactiveGisu = persist(Gisu.create(10008L, START, END, false));
        activeChapter = persist(Chapter.create(activeGisu, "커버리지-서울"));
        inactiveChapter = persist(Chapter.create(inactiveGisu, "커버리지-경기"));
        alphaSchool = School.create("가나다대학교", "첫 학교");
        alphaSchool.updateLogoImageId("logo-alpha");
        alphaSchool = persist(alphaSchool);
        betaSchool = persist(School.create("라마바대학교", "둘째 학교"));
        activeAssignment = persist(ChapterSchool.create(activeChapter, alphaSchool));
        inactiveAssignment = persist(ChapterSchool.create(inactiveChapter, betaSchool));
        persist(SchoolLink.create(alphaSchool, "인스타", SchoolLinkType.INSTAGRAM, "https://instagram.test"));
        persist(SchoolLink.create(alphaSchool, "유튜브", SchoolLinkType.YOUTUBE, "https://youtube.test"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("학교 목록은 키워드·활성 지부 필터·페이징과 전체 개수를 함께 계산한다")
    void 학교_목록을_검색한다() {
        var firstPage = schoolQueryRepository.getSchools(
            new SchoolSearchCondition(null, null), PageRequest.of(0, 1)
        );
        var keywordPage = schoolQueryRepository.getSchools(
            new SchoolSearchCondition("가나다", activeChapter.getId()), PageRequest.of(0, 10)
        );
        var inactiveChapterPage = schoolQueryRepository.getSchools(
            new SchoolSearchCondition(null, inactiveChapter.getId()), PageRequest.of(0, 10)
        );

        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(keywordPage).singleElement().satisfies(info -> {
            assertThat(info.schoolId()).isEqualTo(alphaSchool.getId());
            assertThat(info.chapterId()).isEqualTo(activeChapter.getId());
            assertThat(info.isActive()).isTrue();
        });
        assertThat(inactiveChapterPage).isEmpty();
    }

    @Test
    @DisplayName("기수 날짜 조회는 시작일과 종료일을 포함하는 기수를 반환한다")
    void 날짜로_기수를_조회한다() {
        assertThat(gisuQueryRepository.findGisuByDate(START)).isPresent();
        assertThat(gisuQueryRepository.findGisuByDate(Instant.parse("2100-01-01T00:00:00Z"))).isEmpty();
    }

    @Test
    @DisplayName("학교 상세·기수별·ID 일괄 조회는 활성 기수 여부와 소속 정보를 보존한다")
    void 학교_상세를_여러_조건으로_조회한다() {
        var detail = schoolQueryRepository.getSchoolDetail(alphaSchool.getId());
        var byGisu = schoolQueryRepository.getSchoolDetailsByGisuId(activeGisu.getId());
        var byGisus = schoolQueryRepository.getSchoolDetailsByGisuIds(
            Set.of(activeGisu.getId(), inactiveGisu.getId())
        );
        var byIds = schoolQueryRepository.getSchoolDetailsByIds(
            Set.of(alphaSchool.getId(), betaSchool.getId())
        );

        assertThat(detail.schoolName()).isEqualTo("가나다대학교");
        assertThat(byGisu).anySatisfy(info -> assertThat(info.schoolId()).isEqualTo(alphaSchool.getId()));
        assertThat(byGisus).anySatisfy(info -> assertThat(info.schoolId()).isEqualTo(alphaSchool.getId()));
        assertThat(byIds).hasSize(2);
        assertThat(schoolQueryRepository.getSchoolDetailsByGisuIds(Set.of())).isEmpty();
        assertThat(schoolQueryRepository.getSchoolDetailsByIds(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("학교 엔티티·이름·링크 조회는 fetch join과 링크 그룹화를 수행한다")
    void 학교와_링크를_조회한다() {
        var schools = schoolQueryRepository.findSchoolsByGisuId(activeGisu.getId());
        var names = schoolQueryRepository.findAllNames();
        var links = schoolQueryRepository.findLinksBySchoolId(alphaSchool.getId());
        var linkMap = schoolQueryRepository.findLinksBySchoolIds(
            List.of(alphaSchool.getId(), betaSchool.getId())
        );

        assertThat(schools).anySatisfy(school -> {
            if (school.getId().equals(alphaSchool.getId())) {
                assertThat(school.getChapterSchools()).hasSize(1);
            }
        });
        assertThat(names).extracting(info -> info.schoolName()).contains("가나다대학교", "라마바대학교");
        assertThat(links).extracting(item -> item.title()).containsExactlyInAnyOrder("인스타", "유튜브");
        assertThat(linkMap.get(alphaSchool.getId())).hasSize(2);
        assertThat(linkMap).doesNotContainKey(betaSchool.getId());
        assertThat(schoolQueryRepository.findLinksBySchoolIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("지부-학교 저장소는 단건·학교·기수 조합을 모두 조회하고 연관을 fetch join한다")
    void 지부_학교_소속을_조회한다() {
        assertThat(chapterSchoolQueryRepository.findByChapterIdAndSchoolId(
            activeChapter.getId(), alphaSchool.getId()
        )).isPresent();
        assertThat(chapterSchoolQueryRepository.findByChapterIdAndSchoolId(-1L, -1L)).isEmpty();
        assertThat(chapterSchoolQueryRepository.findBySchoolId(alphaSchool.getId()))
            .extracting(ChapterSchool::getId).containsExactly(activeAssignment.getId());
        assertThat(chapterSchoolQueryRepository.findBySchoolIdIn(
            List.of(alphaSchool.getId(), betaSchool.getId())
        )).hasSize(2);
        assertThat(chapterSchoolQueryRepository.findByGisuIdInAndSchoolIdIn(
            Set.of(activeGisu.getId()), Set.of(alphaSchool.getId())
        )).singleElement().satisfies(assignment -> {
            assertThat(assignment.getChapter().getGisu().getId()).isEqualTo(activeGisu.getId());
            assertThat(assignment.getSchool().getName()).isEqualTo("가나다대학교");
        });
        assertThat(chapterSchoolQueryRepository.findByGisuIdIn(
            Set.of(activeGisu.getId(), inactiveGisu.getId())
        )).extracting(ChapterSchool::getId)
            .contains(activeAssignment.getId(), inactiveAssignment.getId());
    }

    @Test
    @DisplayName("프로덕트 멤버 검색은 null 조건과 활동일·챕터·직책·리더십·스쿼드 조건을 조합한다")
    void 프로덕트_멤버를_모든_조건으로_검색한다() {
        UmcProductChapter chapter = persist(UmcProductChapter.create(
            "SERVER-QUERY", "Server Query", "설명", 1, true
        ));
        UmcProductMember member = persist(UmcProductMember.create(1000L, "소개", null));
        UmcProductMemberActivityPeriod period = persist(UmcProductMemberActivityPeriod.create(
            member, START_DATE, END_DATE
        ));
        persist(UmcProductChapterMembership.create(
            period, chapter, UmcProductPosition.SERVER_DEVELOPER, "Backend", null, START_DATE, END_DATE
        ));
        persist(UmcProductLeadership.create(
            period, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, START_DATE, END_DATE
        ));
        UmcProductSquad squad = persist(UmcProductSquad.create(
            "QUERY-SQUAD", "Query Squad", "설명", START_DATE, END_DATE, 1, true
        ));
        persist(UmcProductSquadParticipant.create(
            squad, period, UmcProductSquadRole.MEMBER, UmcProductPosition.SERVER_DEVELOPER,
            "Backend", null, START_DATE, END_DATE
        ));
        em.flush();
        em.clear();

        assertSearchContains(null, member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(null, null, null, null, START_DATE), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(chapter.getId(), null, null, null, null), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(
            null, null, UmcProductPosition.SERVER_DEVELOPER, null, END_DATE
        ), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(
            null, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, null, null, null
        ), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(
            null, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, null, null, START_DATE
        ), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(
            null, null, null, squad.getId(), null
        ), member.getId());
        assertSearchContains(new UmcProductMemberSearchCondition(
            null, null, null, squad.getId(), END_DATE
        ), member.getId());
        assertThat(memberQueryRepository.searchMemberIds(
            new UmcProductMemberSearchCondition(null, null, null, null, LocalDate.of(2030, 1, 1)),
            PageRequest.of(0, 10)
        )).doesNotContain(member.getId());
    }

    private void assertSearchContains(UmcProductMemberSearchCondition condition, Long memberId) {
        assertThat(memberQueryRepository.searchMemberIds(condition, PageRequest.of(0, 10)))
            .contains(memberId);
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
