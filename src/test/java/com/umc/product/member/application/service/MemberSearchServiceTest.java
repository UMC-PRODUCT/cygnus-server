package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberResult;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class MemberSearchServiceTest {

    private static final Long REQUESTER_MEMBER_ID = 1L;

    @Mock
    SearchMemberPort searchMemberPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    CheckChallengerHistoryUseCase checkChallengerHistoryUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    MemberSearchService memberSearchService;

    private SearchMemberQuery defaultQuery;
    private List<Challenger> sixChallengers;
    private Map<Long, MemberInfo> profiles;
    private List<GisuInfo> defaultGisuInfos;

    @BeforeEach
    void setUp() {
        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(true);
        defaultQuery = new SearchMemberQuery(null, null, null, null, null);
        defaultGisuInfos = List.of(
            new GisuInfo(1L, 7L, Instant.now(), Instant.now(), true),
            new GisuInfo(2L, 8L, Instant.now(), Instant.now(), false)
        );

        sixChallengers = List.of(
            createChallenger(1L, 10L, ChallengerPart.PLAN, 1L),
            createChallenger(2L, 11L, ChallengerPart.DESIGN, 1L),
            createChallenger(3L, 12L, ChallengerPart.WEB, 1L),
            createChallenger(4L, 10L, ChallengerPart.IOS, 2L),   // memberId 10은 2개 기수
            createChallenger(5L, 13L, ChallengerPart.ANDROID, 2L),
            createChallenger(6L, 14L, ChallengerPart.SPRINGBOOT, 2L)
        );

        profiles = Map.of(
            10L, createProfile(10L, "홍길동", "hong"),
            11L, createProfile(11L, "김철수", "kim"),
            12L, createProfile(12L, "이영희", "lee"),
            13L, createProfile(13L, "박민수", "park"),
            14L, createProfile(14L, "최지은", "choi")
        );
    }

    private Challenger createChallenger(Long id, Long memberId, ChallengerPart part, Long gisuId) {
        Challenger challenger = Challenger.builder()
            .memberId(memberId)
            .part(part)
            .gisuId(gisuId)
            .build();
        ReflectionTestUtils.setField(challenger, "id", id);
        return challenger;
    }

    private MemberInfo createProfile(Long id, String name, String nickname) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .nickname(nickname)
            .email("umcproduct@hanyang.ac.kr")
            .schoolId(1L)
            .schoolName("한양대학교 ERICA")
            .status(MemberStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("search")
    class SearchTest {

        @Test
        @DisplayName("기존 REST V2 검색은 챌린저 이력을 확인한 뒤 비범위 포트를 호출한다")
        void 기존_rest_v2_검색은_챌린저_이력_확인_후_비범위_포트를_호출한다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(searchMemberPort.searchMemberIds(defaultQuery, pageable))
                .willReturn(Page.empty(pageable));

            // when
            var result = memberSearchService.searchByV2(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            assertThat(result.page()).isEmpty();
            then(checkChallengerHistoryUseCase).should().hasChallengerHistory(REQUESTER_MEMBER_ID);
            then(searchMemberPort).should().searchMemberIds(defaultQuery, pageable);
        }

        @Test
        void 첫_페이지_조회_시_정상적으로_결과를_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 4);
            List<Challenger> pageContent = sixChallengers.subList(0, 4);
            Page<Challenger> page = new PageImpl<>(pageContent, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(profiles);
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            assertThat(result.page().getContent()).hasSize(4);
            assertThat(result.page().getTotalElements()).isEqualTo(6);
            assertThat(result.page().getTotalPages()).isEqualTo(2);
        }

        @Test
        void 동일_회원의_다른_기수_챌린저가_각각_별도_행으로_반환된다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> page = new PageImpl<>(sixChallengers, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(profiles);
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            List<SearchMemberItemInfo> content = result.page().getContent();
            assertThat(content).hasSize(6);

            // memberId=10인 챌린저가 2개 (challengerId 1, 4)
            long memberTenCount = content.stream()
                .filter(item -> item.memberId().equals(10L))
                .count();
            assertThat(memberTenCount).isEqualTo(2);
        }

        @Test
        void 챌린저별_역할_정보가_올바르게_매핑된다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> page = new PageImpl<>(sixChallengers, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(profiles);
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of(
                1L, List.of(ChallengerRoleType.CENTRAL_PRESIDENT),
                3L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT, ChallengerRoleType.SCHOOL_PART_LEADER)
            ));
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            List<SearchMemberItemInfo> content = result.page().getContent();
            assertThat(content.get(0).roleTypes())
                .containsExactly(ChallengerRoleType.CENTRAL_PRESIDENT);
            assertThat(content.get(1).roleTypes()).isEmpty();
            assertThat(content.get(2).roleTypes())
                .containsExactlyInAnyOrder(ChallengerRoleType.SCHOOL_PRESIDENT, ChallengerRoleType.SCHOOL_PART_LEADER);
            assertThat(content.get(3).roleTypes()).isEmpty();
        }

        @Test
        void gisuId에_따라_기수_번호가_올바르게_매핑된다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> page = new PageImpl<>(sixChallengers, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(profiles);
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            List<SearchMemberItemInfo> content = result.page().getContent();
            // gisuId=1 → generation=7
            assertThat(content.get(0).gisu()).isEqualTo(7L);
            assertThat(content.get(1).gisu()).isEqualTo(7L);
            assertThat(content.get(2).gisu()).isEqualTo(7L);
            // gisuId=2 → generation=8
            assertThat(content.get(3).gisu()).isEqualTo(8L);
            assertThat(content.get(4).gisu()).isEqualTo(8L);
            assertThat(content.get(5).gisu()).isEqualTo(8L);
        }

        @Test
        void 멤버_프로필이_없으면_null로_채워진다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> page = new PageImpl<>(sixChallengers, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(Map.of());
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            List<SearchMemberItemInfo> content = result.page().getContent();
            assertThat(content.get(0).name()).isNull();
            assertThat(content.get(0).nickname()).isNull();
            assertThat(content.get(0).email()).isNull();
            assertThat(content.get(0).schoolName()).isNull();
        }

        @Test
        void 조회_결과가_없으면_빈_페이지를_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(searchMemberPort.search(any(), any())).willReturn(emptyPage);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            assertThat(result.page().getContent()).isEmpty();
            assertThat(result.page().getTotalElements()).isZero();
        }

        @Test
        void 회원_정보와_챌린저_정보가_올바르게_조합된다() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Challenger> page = new PageImpl<>(sixChallengers, pageable, 6);

            given(searchMemberPort.search(any(), any())).willReturn(page);
            given(getMemberUseCase.findAllByIds(anySet())).willReturn(profiles);
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(anySet())).willReturn(Map.of());
            given(getGisuUseCase.getByIds(anySet())).willReturn(defaultGisuInfos);

            // when
            SearchMemberResult result = memberSearchService.searchBy(defaultQuery, REQUESTER_MEMBER_ID, pageable);

            // then
            SearchMemberItemInfo first = result.page().getContent().get(0);
            assertThat(first.memberId()).isEqualTo(10L);
            assertThat(first.name()).isEqualTo("홍길동");
            assertThat(first.nickname()).isEqualTo("hong");
            assertThat(first.email()).isEqualTo("umcproduct@hanyang.ac.kr");
            assertThat(first.schoolName()).isEqualTo("한양대학교 ERICA");
            assertThat(first.challengerId()).isEqualTo(1L);
            assertThat(first.gisuId()).isEqualTo(1L);
            assertThat(first.part()).isEqualTo(ChallengerPart.PLAN);
        }
    }
}
