package com.umc.product.member.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;
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
    @DisplayName("기존 회원 검색은 중복 챌린저를 제거한 안정 순서와 전체 개수로 페이지한다")
    void 기존_회원_검색은_distinct_회원_기준으로_페이지한다() {
        // given
        School school = persistSchool("기존대학교");
        Member first = persistMember("가회원", "first", "first@test.com", school.getId());
        Member second = persistMember("나회원", "second", "second@test.com", school.getId());
        Member third = persistMember("다회원", "third", "third@test.com", school.getId());
        persistChallenger(first.getId(), ChallengerPart.PLAN, 1L);
        persistChallenger(first.getId(), ChallengerPart.WEB, 2L);
        persistChallenger(second.getId(), ChallengerPart.DESIGN, 1L);
        persistChallenger(third.getId(), ChallengerPart.SPRINGBOOT, 1L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);

        // when
        var page = sut.searchMemberIdsBy(query, PageRequest.of(0, 2));

        // then
        assertThat(page.getContent()).containsExactly(first.getId(), second.getId());
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("기수 범위는 해당 기수 챌린저가 있는 회원만 집계한다")
    void 기수_범위는_해당_기수_회원만_집계한다() {
        School school = persistSchool("기수대학교");
        Member allowed = persistMember("가허용", "allowed", "allowed-gisu@test.com", school.getId());
        Member denied = persistMember("나제한", "denied", "denied-gisu@test.com", school.getId());
        persistChallenger(allowed.getId(), ChallengerPart.PLAN, 3L);
        persistChallenger(denied.getId(), ChallengerPart.PLAN, 9L);
        em.flush();
        em.clear();

        var page = sut.searchMemberIdsBy(emptyQuery(),
            MemberSearchAccessScope.restrictedTo(Set.of(), Set.of(3L)),
            PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(allowed.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("학교 범위는 챌린저 이력이 없는 회원도 기수와 무관하게 포함한다")
    void 학교_범위는_챌린저_없는_회원도_포함한다() {
        School allowedSchool = persistSchool("허용대학교");
        School deniedSchool = persistSchool("제한대학교");
        Member withoutChallenger = persistMember("가무이력", "none", "none@test.com", allowedSchool.getId());
        Member otherGisu = persistMember("나타기수", "other", "other@test.com", allowedSchool.getId());
        Member outside = persistMember("다외부", "outside", "outside@test.com", deniedSchool.getId());
        persistChallenger(otherGisu.getId(), ChallengerPart.WEB, 99L);
        persistChallenger(outside.getId(), ChallengerPart.WEB, 3L);
        em.flush();
        em.clear();

        var page = sut.searchMemberIdsBy(emptyQuery(),
            MemberSearchAccessScope.restrictedTo(Set.of(allowedSchool.getId()), Set.of()),
            PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(withoutChallenger.getId(), otherGisu.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("학교와 기수 범위는 OR로 결합하고 중복 챌린저는 한 회원으로 집계한다")
    void 학교와_기수_범위는_or이고_중복은_distinct로_집계한다() {
        School allowedSchool = persistSchool("학교범위대학교");
        School outsideSchool = persistSchool("외부대학교");
        Member schoolMember = persistMember("가학교", "school", "school@test.com", allowedSchool.getId());
        Member gisuMember = persistMember("나기수", "gisu", "gisu@test.com", outsideSchool.getId());
        Member denied = persistMember("다제한", "denied", "denied@test.com", outsideSchool.getId());
        persistChallenger(schoolMember.getId(), ChallengerPart.PLAN, 3L);
        persistChallenger(schoolMember.getId(), ChallengerPart.WEB, 4L);
        persistChallenger(gisuMember.getId(), ChallengerPart.DESIGN, 3L);
        persistChallenger(denied.getId(), ChallengerPart.IOS, 9L);
        em.flush();
        em.clear();

        var page = sut.searchMemberIdsBy(emptyQuery(),
            MemberSearchAccessScope.restrictedTo(Set.of(allowedSchool.getId()), Set.of(3L)),
            PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(schoolMember.getId(), gisuMember.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("무제한 범위는 기존 회원 검색의 내용과 개수를 보존한다")
    void 무제한_범위는_기존_검색과_같다() {
        School school = persistSchool("무제한대학교");
        Member first = persistMember("가회원", "first-all", "first-all@test.com", school.getId());
        Member second = persistMember("나회원", "second-all", "second-all@test.com", school.getId());
        persistChallenger(first.getId(), ChallengerPart.PLAN, 1L);
        persistChallenger(first.getId(), ChallengerPart.WEB, 2L);
        persistChallenger(second.getId(), ChallengerPart.DESIGN, 1L);
        em.flush();
        em.clear();

        var oldPage = sut.searchMemberIdsBy(emptyQuery(), PageRequest.of(0, 10));
        var scopedPage = sut.searchMemberIdsBy(emptyQuery(), MemberSearchAccessScope.allowAll(),
            PageRequest.of(0, 10));

        assertThat(scopedPage.getContent()).isEqualTo(oldPage.getContent());
        assertThat(scopedPage.getTotalElements()).isEqualTo(oldPage.getTotalElements());
    }

    @Test
    @DisplayName("사용자 필터는 접근 범위와 AND로 결합한다")
    void 사용자_필터는_scope와_and로_결합한다() {
        School school = persistSchool("필터대학교");
        Member matched = persistMember("검색허용", "match", "match@test.com", school.getId());
        Member keywordMiss = persistMember("다른이름", "other-filter", "other-filter@test.com", school.getId());
        Member scopeMiss = persistMember("검색제한", "match-denied", "match-denied@test.com", school.getId());
        persistChallenger(matched.getId(), ChallengerPart.PLAN, 3L);
        persistChallenger(keywordMiss.getId(), ChallengerPart.PLAN, 3L);
        persistChallenger(scopeMiss.getId(), ChallengerPart.PLAN, 9L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery("검색", null, null, null, null);
        var page = sut.searchMemberIdsBy(query,
            MemberSearchAccessScope.restrictedTo(Set.of(), Set.of(3L)),
            PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(matched.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("제한 범위 페이지의 크기 다음 페이지 여부 전체 개수는 DB 범위 집계를 따른다")
    void 제한_범위_페이지_메타데이터는_db_집계를_따른다() {
        School school = persistSchool("페이지대학교");
        for (int index = 1; index <= 4; index++) {
            Member member = persistMember("회원" + index, "page" + index, "page" + index + "@test.com", school.getId());
            persistChallenger(member.getId(), ChallengerPart.PLAN, index <= 3 ? 3L : 9L);
        }
        em.flush();
        em.clear();

        var page = sut.searchMemberIdsBy(emptyQuery(),
            MemberSearchAccessScope.restrictedTo(Set.of(), Set.of(3L)),
            PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("방어적으로 거부 범위가 저장소에 전달되어도 빈 페이지를 반환한다")
    void 거부_범위는_빈_페이지를_반환한다() {
        School school = persistSchool("거부대학교");
        Member member = persistMember("거부회원", "denied-scope", "denied-scope@test.com", school.getId());
        persistChallenger(member.getId(), ChallengerPart.PLAN, 3L);
        em.flush();
        em.clear();

        var page = sut.searchMemberIdsBy(emptyQuery(), MemberSearchAccessScope.denyAll(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

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
    @DisplayName("gisuId 필터에 해당하는 기수의 챌린저만 검색한다")
    void gisuId_필터에_해당하는_기수의_챌린저만_검색한다() {
        School school = persistSchool("테스트대학교");
        Member first = persistMember("김일", "one", "one@test.com", school.getId());
        Member second = persistMember("김이", "two", "two@test.com", school.getId());
        Challenger allowed = persistChallenger(first.getId(), ChallengerPart.PLAN, 10L);
        Challenger denied = persistChallenger(second.getId(), ChallengerPart.PLAN, 20L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery(null, 10L, null, null, null);
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
    @DisplayName("schoolId 필터에 해당하는 학교의 회원만 검색한다")
    void schoolId_필터에_해당하는_학교의_회원만_검색한다() {
        School allowedSchool = persistSchool("허용대학교");
        School deniedSchool = persistSchool("제한대학교");
        Member allowedMember = persistMember("박허용", "allowed", "allowed@test.com", allowedSchool.getId());
        Member deniedMember = persistMember("박제한", "denied", "denied@test.com", deniedSchool.getId());
        Challenger allowed = persistChallenger(allowedMember.getId(), ChallengerPart.SPRINGBOOT, 10L);
        Challenger denied = persistChallenger(deniedMember.getId(), ChallengerPart.SPRINGBOOT, 10L);
        em.flush();
        em.clear();

        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, allowedSchool.getId());
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

    @Test
    @DisplayName("초대 후보는 Challenger 이력과 무관하게 활성 회원만 제외 목록과 이름으로 페이지한다")
    void 초대_후보는_활성_회원_기준으로_페이지한다() {
        // given
        School school = persistSchool("초대대학교");
        Member blocked = persistMember("검색가", "blocked", "blocked@test.com", school.getId());
        Member withoutChallenger = persistMember("검색나", "none", "invite-none@test.com", school.getId());
        Member withChallenger = persistMember("검색다", "history", "invite-history@test.com", school.getId());
        Member inactive = persistMember("검색라", "inactive", "invite-inactive@test.com", school.getId());
        ReflectionTestUtils.setField(inactive, "status", MemberStatus.INACTIVE);
        persistChallenger(withChallenger.getId(), ChallengerPart.WEB, 9L);
        em.flush();
        em.clear();

        // when
        var firstPage = sut.searchInvitationCandidates(new SearchMemberInvitationCondition(
            "검색", Set.of(blocked.getId()), 0, 1
        ));
        var secondPage = sut.searchInvitationCandidates(new SearchMemberInvitationCondition(
            "검색", Set.of(blocked.getId()), 1, 1
        ));
        Set<Long> activeMemberIds = sut.findActiveMemberIds(Set.of(
            blocked.getId(),
            withoutChallenger.getId(),
            withChallenger.getId(),
            inactive.getId(),
            Long.MAX_VALUE
        ));

        // then
        assertThat(firstPage.items())
            .extracting(candidate -> candidate.memberId())
            .containsExactly(withoutChallenger.getId());
        assertThat(firstPage.total()).isEqualTo(2L);
        assertThat(secondPage.items())
            .extracting(candidate -> candidate.memberId())
            .containsExactly(withChallenger.getId());
        assertThat(secondPage.total()).isEqualTo(2L);
        assertThat(activeMemberIds).containsExactlyInAnyOrder(
            blocked.getId(),
            withoutChallenger.getId(),
            withChallenger.getId()
        );
    }

    private School persistSchool(String name) {
        School school = School.create(name, null, null);
        em.persist(school);
        return school;
    }

    private SearchMemberQuery emptyQuery() {
        return new SearchMemberQuery(null, null, null, null, null);
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
