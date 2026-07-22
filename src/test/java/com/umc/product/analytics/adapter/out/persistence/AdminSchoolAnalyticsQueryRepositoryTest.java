package com.umc.product.analytics.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryQuery;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.analytics.domain.AdminAnalyticsSort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({AdminSchoolAnalyticsQueryRepository.class})
@DisplayName("AdminSchoolAnalyticsQueryRepository")
class AdminSchoolAnalyticsQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AdminSchoolAnalyticsQueryRepository sut;

    private Long gisuId;
    private Long chapterAId;
    private Long chapterBId;
    private Long schoolAId;
    private Long schoolBId;

    @BeforeEach
    void setUp() {
        // 시드 마이그레이션이 이미 is_active=true 인 10기를 넣어두므로 부분 unique index uq_gisu_active 와
        // 충돌하지 않도록 비활성 기수로 만든다. 분석 쿼리는 scope.gisuId() 로 직접 스코프한다.
        Gisu gisu = em.persist(Gisu.create(7L, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400), false));
        Chapter chapterA = em.persist(Chapter.create(gisu, "A지부"));
        Chapter chapterB = em.persist(Chapter.create(gisu, "B지부"));
        School schoolA = em.persist(School.create("가천대학교", null));
        School schoolB = em.persist(School.create("숭실대학교", null));
        em.persist(ChapterSchool.create(chapterA, schoolA));
        em.persist(ChapterSchool.create(chapterB, schoolB));
        em.flush();

        gisuId = gisu.getId();
        chapterAId = chapterA.getId();
        chapterBId = chapterB.getId();
        schoolAId = schoolA.getId();
        schoolBId = schoolB.getId();
    }

    @Test
    @DisplayName("학교 요약은 위험군 수 내림차순이 기본이다")
    void 학교_요약은_위험군_수_내림차순이_기본이다() {
        Challenger a1 = persistChallenger("가천1", schoolAId, ChallengerPart.SPRINGBOOT);
        Challenger a2 = persistChallenger("가천2", schoolAId, ChallengerPart.WEB);
        Challenger b1 = persistChallenger("숭실1", schoolBId, ChallengerPart.IOS);
        persistPoint(a1, -10);
        persistPoint(a2, -9);
        persistPoint(b1, -8);
        em.flush();
        em.clear();

        Page<AdminSchoolSummaryInfo> result = sut.getSchoolSummaries(
            centralScope(),
            query(null, null, null)
        );

        assertThat(result.getContent())
            .extracting(AdminSchoolSummaryInfo::schoolId)
            .containsExactly(schoolAId, schoolBId);
    }

    @Test
    @DisplayName("지부 스코프에서는 해당 지부의 학교만 조회된다")
    void 지부_스코프에서는_해당_지부의_학교만_조회된다() {
        persistChallenger("가천1", schoolAId, ChallengerPart.SPRINGBOOT);
        persistChallenger("숭실1", schoolBId, ChallengerPart.IOS);
        em.flush();
        em.clear();

        Page<AdminSchoolSummaryInfo> result = sut.getSchoolSummaries(
            chapterScope(chapterAId),
            query(chapterAId, null, null)
        );

        assertThat(result.getContent())
            .extracting(AdminSchoolSummaryInfo::schoolId)
            .containsExactly(schoolAId);
    }

    @Test
    @DisplayName("검색어가 있으면 학교명 부분 일치만 조회된다")
    void 검색어가_있으면_학교명_부분_일치만_조회된다() {
        persistChallenger("가천1", schoolAId, ChallengerPart.SPRINGBOOT);
        persistChallenger("숭실1", schoolBId, ChallengerPart.IOS);
        em.flush();
        em.clear();

        Page<AdminSchoolSummaryInfo> result = sut.getSchoolSummaries(
            centralScope(),
            query(null, "가천", null)
        );

        assertThat(result.getContent())
            .extracting(AdminSchoolSummaryInfo::schoolName)
            .containsExactly("가천대학교");
    }

    @Test
    @DisplayName("파트장 배치율은 배치된 파트수와 운영중인 파트수로 계산된다")
    void 파트장_배치율은_배치된_파트수와_운영중인_파트수로_계산된다() {
        Challenger leader = persistChallenger("파트장", schoolAId, ChallengerPart.SPRINGBOOT);
        persistChallenger("웹챌", schoolAId, ChallengerPart.WEB);
        em.persist(ChallengerRole.create(
            leader.getId(),
            ChallengerRoleType.SCHOOL_PART_LEADER,
            schoolAId,
            ChallengerPart.SPRINGBOOT,
            gisuId
        ));
        em.flush();
        em.clear();

        Page<AdminSchoolSummaryInfo> result = sut.getSchoolSummaries(centralScope(), query(null, null, null));

        AdminSchoolSummaryInfo.PartLeaderRatioInfo ratio = result.getContent().getFirst().partLeaderRatio();
        assertThat(ratio.assigned()).isEqualTo(1L);
        assertThat(ratio.totalRunningParts()).isEqualTo(2L);
    }

    @Test
    @DisplayName("포인트가 없는 챌린저는 0점으로 평균에 포함된다")
    void 포인트가_없는_챌린저는_0점으로_평균에_포함된다() {
        Challenger challenger = persistChallenger("감점", schoolAId, ChallengerPart.SPRINGBOOT);
        persistChallenger("무점", schoolAId, ChallengerPart.WEB);
        persistPoint(challenger, -10);
        em.flush();
        em.clear();

        Page<AdminSchoolSummaryInfo> result = sut.getSchoolSummaries(centralScope(), query(null, null, null));

        assertThat(result.getContent().getFirst().averagePointSum()).isEqualTo(-5.0);
    }

    @Test
    @DisplayName("학교 요약은 운영진 조립·모든 정렬·빈 페이지와 학교·파트 스코프를 처리한다")
    void 운영진_정렬_빈_page와_scope를_처리한다() {
        Challenger president = persistChallenger("회장", schoolAId, ChallengerPart.SPRINGBOOT);
        Challenger vicePresident = persistChallenger("부회장", schoolAId, ChallengerPart.WEB);
        Challenger leader = persistChallenger("파트장", schoolAId, ChallengerPart.SPRINGBOOT);
        em.persist(ChallengerRole.create(
            president.getId(), ChallengerRoleType.SCHOOL_PRESIDENT, schoolAId, null, gisuId
        ));
        em.persist(ChallengerRole.create(
            vicePresident.getId(), ChallengerRoleType.SCHOOL_VICE_PRESIDENT, schoolAId, null, gisuId
        ));
        em.persist(ChallengerRole.create(
            leader.getId(), ChallengerRoleType.SCHOOL_PART_LEADER, schoolAId, ChallengerPart.SPRINGBOOT, gisuId
        ));
        em.flush();
        em.clear();

        for (AdminAnalyticsSort sort : AdminAnalyticsSort.values()) {
            AdminSchoolSummaryQuery direct = new AdminSchoolSummaryQuery(
                1L, gisuId, null, null, -8, PageRequest.of(0, 10), sort
            );
            assertThat(sut.getSchoolSummaries(centralScope(), direct)).isNotEmpty();
        }
        AdminSchoolSummaryInfo first = sut.getSchoolSummaries(centralScope(), query(null, null, null))
            .getContent().getFirst();
        assertThat(first.president().name()).isEqualTo("회장");
        assertThat(first.vicePresident().name()).isEqualTo("부회장");

        AdminSchoolSummaryQuery beyond = new AdminSchoolSummaryQuery(
            1L, gisuId, null, null, -8, PageRequest.of(100, 10),
            AdminAnalyticsSort.RISK_CHALLENGER_COUNT_DESC
        );
        assertThat(sut.getSchoolSummaries(centralScope(), beyond)).isEmpty();
        assertThat(sut.getSchoolSummaries(centralScope(), query(null, "존재하지않음", null))).isEmpty();

        AdminAnalyticsScope partScope = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.SCHOOL_PART, gisuId, null, schoolAId, ChallengerPart.SPRINGBOOT,
            ChallengerRoleType.SCHOOL_PART_LEADER
        );
        assertThat(sut.getSchoolSummaries(partScope, query(null, null, null)))
            .extracting(AdminSchoolSummaryInfo::schoolId).containsExactly(schoolAId);
    }

    private AdminSchoolSummaryQuery query(Long chapterId, String search, String sort) {
        return AdminSchoolSummaryQuery.of(1L, gisuId, chapterId, search, -8, PageRequest.of(0, 10), sort);
    }

    private Challenger persistChallenger(String name, Long schoolId, ChallengerPart part) {
        Member member = em.persist(Member.create(name, name + "닉", name + "@example.com", schoolId, null));
        return em.persist(new Challenger(member.getId(), part, gisuId));
    }

    private void persistPoint(Challenger challenger, int value) {
        em.persist(ChallengerPoint.create(challenger, PointType.CUSTOM, value, "테스트"));
    }

    private AdminAnalyticsScope centralScope() {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL,
            gisuId,
            null,
            null,
            null,
            ChallengerRoleType.CENTRAL_PRESIDENT
        );
    }

    private AdminAnalyticsScope chapterScope(Long chapterId) {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CHAPTER,
            gisuId,
            chapterId,
            null,
            null,
            ChallengerRoleType.CHAPTER_PRESIDENT
        );
    }
}
