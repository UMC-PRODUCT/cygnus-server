package com.umc.product.member.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.dto.SearchMemberAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.School;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    MemberQueryRepository.class
})
@DisplayName("MemberQueryRepository 검색")
class MemberQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    MemberQueryRepository sut;

    @Test
    @DisplayName("keyword는 학교명만 일치하는 회원을 검색하지 않는다")
    void keyword는_학교명만_일치하는_회원을_검색하지_않는다() {
        // given
        School school = persistSchool("한양대학교 ERICA");
        Member member = persistMember("홍길동", "hong", "hong@test.com", school.getId());
        Challenger challenger = persistChallenger(member.getId(), ChallengerPart.PLAN, 7L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery("ERICA", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        var challengers = sut.searchBy(query, pageable);
        var memberIds = sut.searchMemberIdsBy(query, pageable);

        // then
        assertThat(challengers.getContent())
            .extracting(Challenger::getId)
            .doesNotContain(challenger.getId());
        assertThat(challengers.getTotalElements()).isZero();
        assertThat(memberIds.getContent()).isEmpty();
        assertThat(memberIds.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("접근 범위에 허용된 기수의 챌린저만 검색한다")
    void 접근_범위에_허용된_기수의_챌린저만_검색한다() {
        School school = persistSchool("테스트대학교");
        Member first = persistMember("김일", "one", "one@test.com", school.getId());
        Member second = persistMember("김이", "two", "two@test.com", school.getId());
        Challenger allowed = persistChallenger(first.getId(), ChallengerPart.PLAN, 10L);
        Challenger denied = persistChallenger(second.getId(), ChallengerPart.PLAN, 20L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null)
            .withAccessScope(SearchMemberAccessScope.ofGisuIds(Set.of(10L)));
        PageRequest pageable = PageRequest.of(0, 10);

        var challengers = sut.searchBy(query, pageable);
        var memberIds = sut.searchMemberIdsBy(query, pageable);

        assertThat(challengers.getContent())
            .extracting(Challenger::getId)
            .containsExactly(allowed.getId());
        assertThat(challengers.getContent())
            .extracting(Challenger::getId)
            .doesNotContain(denied.getId());
        assertThat(memberIds.getContent()).containsExactly(first.getId());
    }

    @Test
    @DisplayName("접근 범위에 허용된 학교의 회원만 검색한다")
    void 접근_범위에_허용된_학교의_회원만_검색한다() {
        School allowedSchool = persistSchool("허용대학교");
        School deniedSchool = persistSchool("제한대학교");
        Member allowedMember = persistMember("박허용", "allowed", "allowed@test.com", allowedSchool.getId());
        Member deniedMember = persistMember("박제한", "denied", "denied@test.com", deniedSchool.getId());
        Challenger allowed = persistChallenger(allowedMember.getId(), ChallengerPart.SPRINGBOOT, 10L);
        Challenger denied = persistChallenger(deniedMember.getId(), ChallengerPart.SPRINGBOOT, 10L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null)
            .withAccessScope(SearchMemberAccessScope.ofSchoolIds(Set.of(allowedSchool.getId())));
        PageRequest pageable = PageRequest.of(0, 10);

        var challengers = sut.searchBy(query, pageable);
        var memberIds = sut.searchMemberIdsBy(query, pageable);

        assertThat(challengers.getContent())
            .extracting(Challenger::getId)
            .containsExactly(allowed.getId());
        assertThat(challengers.getContent())
            .extracting(Challenger::getId)
            .doesNotContain(denied.getId());
        assertThat(memberIds.getContent()).containsExactly(allowedMember.getId());
    }

    private School persistSchool(String name) {
        School school = School.create(name, null);
        em.persist(school);
        return school;
    }

    private Member persistMember(String name, String nickname, String email, Long schoolId) {
        Member member = Member.create(name, nickname, email, schoolId, null);
        em.persist(member);
        return member;
    }

    private Challenger persistChallenger(Long memberId, ChallengerPart part, Long gisuId) {
        Challenger challenger = Challenger.builder()
            .memberId(memberId)
            .part(part)
            .gisuId(gisuId)
            .build();
        em.persist(challenger);
        return challenger;
    }
}
