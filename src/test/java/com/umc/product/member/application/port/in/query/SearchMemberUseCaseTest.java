package com.umc.product.member.application.port.in.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberResult;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.command.SaveGisuPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.UseCaseTestSupport;

@Disabled
class SearchMemberUseCaseTest extends UseCaseTestSupport {

    @Autowired
    private SearchMemberUseCase searchMemberUseCase;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private SaveChallengerPort saveChallengerPort;

    @Autowired
    private SaveChallengerRolePort saveChallengerRolePort;

    @Autowired
    private SaveGisuPort saveGisuPort;

    @Autowired
    private SaveSchoolPort saveSchoolPort;

    @Autowired
    private SaveChapterPort saveChapterPort;

    private Gisu gisu7;
    private Gisu gisu8;
    private School school1;
    private School school2;
    private Chapter chapter;
    private Long requesterMemberId;

    @BeforeEach
    void setUp() {
        requesterMemberId = null;
        gisu7 = saveGisuPort.save(Gisu.create(7L,
            Instant.parse("2024-03-01T00:00:00Z"),
            Instant.parse("2024-08-31T23:59:59Z"),
            true));
        gisu8 = saveGisuPort.save(Gisu.create(8L,
            Instant.parse("2024-09-01T00:00:00Z"),
            Instant.parse("2025-02-28T23:59:59Z"),
            true));

        school1 = saveSchoolPort.save(School.create("한양대학교 ERICA", null));
        school2 = saveSchoolPort.save(School.create("한성대학교", null));

        chapter = saveChapterPort.save(Chapter.create(gisu8, "Scorpio"));
        school1.updateChapterSchool(chapter);
        saveSchoolPort.save(school1);
    }

    private Member saveMember(String name, String nickname, String email, Long schoolId) {
        Member member = saveMemberPort.save(Member.builder()
            .name(name)
            .nickname(nickname)
            .email(email)
            .schoolId(schoolId)
            .build());

        if (requesterMemberId == null) {
            requesterMemberId = member.getId();
        }

        return member;
    }

    @Nested
    @DisplayName("조건 없이 전체 검색")
    class NoConditionSearch {

        @Test
        void 전체_회원을_조회한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school2.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(2);
            assertThat(result.page().getTotalElements()).isEqualTo(2);
        }

        @Test
        void 한_회원이_여러_기수에_활동하면_각각_별도_행으로_반환된다() {
            // given
            Member member = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member.getId()).part(ChallengerPart.WEB).gisuId(gisu8.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(2);
            assertThat(result.page().getContent())
                .allMatch(item -> item.memberId().equals(member.getId()));
        }
    }

    @Nested
    @DisplayName("키워드 검색")
    class KeywordSearch {

        @Test
        void 이름으로_검색한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery("홍길동", null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동");
        }

        @Test
        void 이메일로_검색한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@umc.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery("umc.com", null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).email()).isEqualTo("hong@umc.com");
        }

        @Test
        void 학교명으로_검색한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school2.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery("ERICA", null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동");
        }

        @Test
        void 닉네임으로_검색한다() {
            // given
            Member member1 = saveMember("홍길동", "hong123", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim456", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery("hong123", null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).nickname()).isEqualTo("hong123");
        }

        @Test
        void 검색_결과가_없으면_빈_페이지를_반환한다() {
            // given
            Member member = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery("존재하지않는검색어", null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).isEmpty();
            assertThat(result.page().getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("필터링")
    class FilterSearch {

        @Test
        void 기수로_필터링한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu8.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, gisu7.getId(), null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동");
        }

        @Test
        void 파트로_필터링한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, null, ChallengerPart.WEB, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("김철수");
            assertThat(result.page().getContent().get(0).part()).isEqualTo(ChallengerPart.WEB);
        }

        @Test
        void 학교로_필터링한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school2.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, school2.getId());
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("김철수");
        }

        @Test
        void 지부로_필터링한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId()); // school1 → chapter (Scorpio)
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school2.getId()); // school2 → 지부 없음
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu7.getId())
                    .build());

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, chapter.getId(), null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동");
        }

        @Test
        void 여러_필터를_동시에_적용한다() {
            // given
            Member member1 = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Member member2 = saveMember("김철수", "kim", "kim@test.com", school1.getId());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member1.getId()).part(ChallengerPart.WEB).gisuId(gisu8.getId())
                    .build());
            saveChallengerPort.save(
                Challenger.builder().memberId(member2.getId()).part(ChallengerPart.WEB).gisuId(gisu8.getId())
                    .build());

            // gisu8 + WEB 파트 필터
            SearchMemberQuery query = new SearchMemberQuery(null, gisu8.getId(), ChallengerPart.WEB, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(2);
            assertThat(result.page().getContent())
                .allMatch(item -> item.part() == ChallengerPart.WEB);
        }
    }

    @Nested
    @DisplayName("역할 정보 매핑")
    class RoleMapping {

        @Test
        void 챌린저_역할이_정상적으로_포함된다() {
            // given
            Member member = saveMember("홍길동", "hong", "hong@test.com", school1.getId());
            Challenger challenger = saveChallengerPort.save(
                Challenger.builder().memberId(member.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId()).build()
            );
            saveChallengerRolePort.save(ChallengerRole.create(
                challenger.getId(), ChallengerRoleType.SCHOOL_PRESIDENT, school1.getId(), null, gisu7.getId()
            ));

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            SearchMemberResult result = searchMemberUseCase.searchBy(query, requesterMemberId, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(1);
            assertThat(result.page().getContent().get(0).roleTypes())
                .containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT);
        }
    }

    // ── 헬퍼 메서드 ──

    @Nested
    @DisplayName("페이징")
    class Paging {

        @Test
        void 페이징이_정상_동작한다() {
            // given
            for (int i = 1; i <= 5; i++) {
                Member member = saveMember("회원" + i, "nick" + i, "member" + i + "@test.com", school1.getId());
                saveChallengerPort.save(
                    Challenger.builder().memberId(member.getId()).part(ChallengerPart.PLAN).gisuId(gisu7.getId())
                        .build());
            }

            SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
            PageRequest firstPage = PageRequest.of(0, 3);
            PageRequest secondPage = PageRequest.of(1, 3);

            // when
            SearchMemberResult firstResult = searchMemberUseCase.searchBy(query, requesterMemberId, firstPage);
            SearchMemberResult secondResult = searchMemberUseCase.searchBy(query, requesterMemberId, secondPage);

            // then
            assertThat(firstResult.page().getContent()).hasSize(3);
            assertThat(firstResult.page().getTotalElements()).isEqualTo(5);
            assertThat(firstResult.page().getTotalPages()).isEqualTo(2);
            assertThat(firstResult.page().hasNext()).isTrue();

            assertThat(secondResult.page().getContent()).hasSize(2);
            assertThat(secondResult.page().hasNext()).isFalse();
        }
    }
}
