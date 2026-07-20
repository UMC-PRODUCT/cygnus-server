package com.umc.product.challenger.adapter.out.persistence;

import static com.umc.product.member.domain.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(ChallengerQueryRepository.class)
@DisplayName("ChallengerQueryRepository 통합 검색")
class ChallengerQueryRepositoryIntegrationTest {

    @Autowired TestEntityManager em;
    @Autowired ChallengerQueryRepository sut;

    School school;
    Gisu oldGisu;
    Gisu latestGisu;
    Chapter chapter;
    Challenger oldChallenger;
    Challenger latestChallenger;
    Challenger anotherChallenger;

    @BeforeEach
    void setUp() {
        school = em.persist(School.create("테스트대학교", null));
        oldGisu = em.persist(Gisu.create(
            11L,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-06-30T00:00:00Z"),
            false
        ));
        latestGisu = em.persist(Gisu.create(
            12L,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-06-30T00:00:00Z"),
            false
        ));
        chapter = em.persist(Chapter.create(latestGisu, "서울"));
        em.persist(ChapterSchool.create(chapter, school));
        Member alpha = em.persist(Member.create(
            "가회원", "alpha", "alpha@example.com", school.getId(), "profile-alpha"));
        Member beta = em.persist(Member.create(
            "나회원", "beta", "beta@example.com", school.getId(), null));
        oldChallenger = persistChallenger(alpha.getId(), ChallengerPart.PLAN, oldGisu.getId());
        latestChallenger = persistChallenger(alpha.getId(), ChallengerPart.WEB, latestGisu.getId());
        anotherChallenger = persistChallenger(beta.getId(), ChallengerPart.DESIGN, latestGisu.getId());
        em.persist(ChallengerPoint.create(latestChallenger, PointType.CUSTOM, 7, "custom"));
        em.persist(ChallengerPoint.create(latestChallenger, PointType.BLOG_CHALLENGE, "blog"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("지부·paging·파트 집계·포인트·최근 기수 검색을 실제 DB 계약으로 검증한다")
    void 주요_조회와_집계를_수행한다() {
        SearchChallengerQuery query = emptyQuery();

        assertThat(sut.listByChapterId(null)).isEmpty();
        assertThat(sut.listByChapterId(chapter.getId()))
            .extracting(Challenger::getId)
            .containsExactlyInAnyOrder(latestChallenger.getId(), anotherChallenger.getId());
        assertThat(sut.pagingSearch(query, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3L);
        assertThat(sut.countByPart(query))
            .containsEntry(ChallengerPart.PLAN, 1L)
            .containsEntry(ChallengerPart.DESIGN, 1L)
            .containsEntry(ChallengerPart.WEB, 1L);
        assertThat(sut.sumPointsByChallengerIds(null)).isEmpty();
        assertThat(sut.sumPointsByChallengerIds(Set.of())).isEmpty();
        assertThat(sut.sumPointsByChallengerIds(Set.of(latestChallenger.getId())))
            .containsEntry(latestChallenger.getId(), 3.0);
        assertThat(sut.getAllLatestGisuPerMember())
            .extracting(Challenger::getId)
            .containsExactlyInAnyOrder(latestChallenger.getId(), anotherChallenger.getId());

        var bundle = sut.pagingSearchWithCounts(query, PageRequest.of(0, 10));
        assertThat(bundle.rows()).hasSize(3);
        assertThat(bundle.partCounts()).containsEntry(ChallengerPart.PLAN, 1L);
    }

    @Test
    @DisplayName("모든 filter 조합과 cursor 검색·잘못된 cursor를 검증한다")
    void filter와_cursor를_검증한다() {
        SearchChallengerQuery allFilters = new SearchChallengerQuery(
            latestChallenger.getId(),
            "가회",
            "alp",
            null,
            school.getId(),
            chapter.getId(),
            ChallengerPart.WEB,
            latestGisu.getId(),
            List.of(ChallengerStatus.ACTIVE)
        );

        assertThat(sut.cursorSearch(allFilters, null, 10))
            .extracting(Challenger::getId)
            .containsExactly(latestChallenger.getId());
        assertThat(sut.cursorSearch(keywordQuery("회원"), null, 10)).hasSize(3);
        assertThat(sut.cursorSearch(nameNicknameQuery(null, "alp"), null, 10)).hasSize(2);
        assertThat(sut.cursorSearch(nameNicknameQuery("나회", null), null, 10)).hasSize(1);
        assertThat(sut.cursorSearch(nameNicknameQuery(" ", " "), null, 10)).hasSize(3);

        assertThat(sut.cursorSearch(emptyQuery(), oldChallenger.getId(), 10)).hasSize(2);
        assertThat(sut.cursorSearchWithCounts(emptyQuery(), oldChallenger.getId(), 10).rows()).hasSize(2);
        assertThatThrownBy(() -> sut.cursorSearch(emptyQuery(), Long.MAX_VALUE, 10))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("지부 존재 조건 helper는 null과 실제 지부를 구분한다")
    void 지부_존재_조건을_구성한다() {
        BooleanExpression nullCondition = ReflectionTestUtils.invokeMethod(
            sut, "chapterIdExists", null, member);
        BooleanExpression condition = ReflectionTestUtils.invokeMethod(
            sut, "chapterIdExists", chapter.getId(), member);

        assertThat(nullCondition).isNull();
        assertThat(condition).isNotNull();
    }

    private SearchChallengerQuery emptyQuery() {
        return new SearchChallengerQuery(null, null, null, null, null, null, null, null, null);
    }

    private SearchChallengerQuery keywordQuery(String keyword) {
        return new SearchChallengerQuery(null, null, null, keyword, null, null, null, null, null);
    }

    private SearchChallengerQuery nameNicknameQuery(String name, String nickname) {
        return new SearchChallengerQuery(null, name, nickname, null, null, null, null, null, null);
    }

    private Challenger persistChallenger(Long memberId, ChallengerPart part, Long gisuId) {
        return em.persist(Challenger.builder()
            .memberId(memberId)
            .part(part)
            .gisuId(gisuId)
            .build());
    }
}
