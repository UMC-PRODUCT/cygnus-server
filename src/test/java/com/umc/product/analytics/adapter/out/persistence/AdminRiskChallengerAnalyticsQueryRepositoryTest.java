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

import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerQuery;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
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
@Import({AdminRiskChallengerAnalyticsQueryRepository.class})
@DisplayName("AdminRiskChallengerAnalyticsQueryRepository")
class AdminRiskChallengerAnalyticsQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AdminRiskChallengerAnalyticsQueryRepository sut;

    private Long gisuId;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        // 시드 마이그레이션이 이미 is_active=true 인 10기를 넣어두므로 부분 unique index uq_gisu_active 와
        // 충돌하지 않도록 비활성 기수로 만든다. 분석 쿼리는 scope.gisuId() 로 직접 스코프한다.
        Gisu gisu = em.persist(Gisu.create(7L, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400), false));
        Chapter chapter = em.persist(Chapter.create(gisu, "중앙"));
        School school = em.persist(School.create("가천대학교", null, null));
        em.persist(ChapterSchool.create(chapter, school));
        em.flush();
        gisuId = gisu.getId();
        schoolId = school.getId();
    }

    @Test
    @DisplayName("pointSumLte가 있으면 포인트 합계가 임계치 이하인 챌린저만 조회한다")
    void pointSumLte가_있으면_포인트_합계가_임계치_이하인_챌린저만_조회한다() {
        Challenger risk = persistChallenger("위험", ChallengerPart.SPRINGBOOT);
        Challenger safe = persistChallenger("정상", ChallengerPart.WEB);
        persistPoint(risk, -10);
        persistPoint(safe, -2);
        em.flush();
        em.clear();

        Page<AdminRiskChallengerInfo> result = sut.getRiskChallengers(scope(), query(-8));

        assertThat(result.getContent())
            .extracting(AdminRiskChallengerInfo::name)
            .containsExactly("위험");
    }

    @Test
    @DisplayName("includeLatestNegativePoint는 페이지 대상자의 최신 감점만 응답한다")
    void includeLatestNegativePoint는_페이지_대상자의_최신_감점만_응답한다() {
        Challenger risk = persistChallenger("위험", ChallengerPart.SPRINGBOOT);
        persistPoint(risk, -4);
        persistPoint(risk, -10);
        em.flush();
        em.clear();

        Page<AdminRiskChallengerInfo> result = sut.getRiskChallengers(scope(), query(-8));

        assertThat(result.getContent().getFirst().latestNegativePoint()).isNotNull();
        assertThat(result.getContent().getFirst().latestNegativePoint().score()).isEqualTo(-10.0);
    }

    private AdminRiskChallengerQuery query(Integer riskThreshold) {
        return AdminRiskChallengerQuery.of(1L, gisuId, null, null, riskThreshold, PageRequest.of(0, 10));
    }

    private Challenger persistChallenger(String name, ChallengerPart part) {
        Member member = em.persist(Member.create(name, name + "닉", name + "@example.com", schoolId, null));
        return em.persist(new Challenger(member.getId(), part, gisuId));
    }

    private void persistPoint(Challenger challenger, int value) {
        em.persist(ChallengerPoint.create(challenger, PointType.CUSTOM, value, "테스트"));
    }

    private AdminAnalyticsScope scope() {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL,
            gisuId,
            null,
            null,
            null,
            ChallengerRoleType.CENTRAL_PRESIDENT
        );
    }
}
