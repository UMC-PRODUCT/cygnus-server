package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchChallengerInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("챌린저 초대 대상 검색 서비스")
class ChallengerInvitationQueryServiceTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Test
    @DisplayName("서로 다른 기수의 활동 중·수료 챌린저를 포함하고 탈퇴·제명 챌린저는 제외한다")
    void 검색은_기수와_무관하게_활동_중과_수료_챌린저를_포함한다() {
        // given
        ChallengerBasicInfo excluded = challenger(1_010L, 10L, 5L, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo active = challenger(2_020L, 20L, 5L, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo graduated = challenger(3_030L, 30L, 3L, ChallengerStatus.GRADUATED);
        ChallengerBasicInfo withdrawn = challenger(4_040L, 40L, 4L, ChallengerStatus.WITHDRAWN);
        ChallengerBasicInfo expelled = challenger(5_050L, 50L, 2L, ChallengerStatus.EXPELLED);
        given(getChallengerUseCase.listLatestBasicPerMember())
            .willReturn(List.of(withdrawn, graduated, excluded, expelled, active));
        given(getMemberUseCase.findAllByIds(Set.of(20L, 30L))).willReturn(Map.of(
            20L, member(20L, "나활동"),
            30L, member(30L, "가수료")
        ));
        given(getGisuUseCase.getByIds(Set.of(3L, 5L))).willReturn(List.of(
            gisuInfo(3L, 7L),
            gisuInfo(5L, 9L)
        ));

        // when
        ChallengerInvitationSearchResult result = service().search(
            query(null, Set.of(10L), 0, 10)
        );

        // then
        assertThat(result.items())
            .extracting(
                ChallengerInvitationInfo::memberId,
                ChallengerInvitationInfo::challengerId,
                ChallengerInvitationInfo::generation
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(30L, 3_030L, 7L),
                org.assertj.core.groups.Tuple.tuple(20L, 2_020L, 9L)
            );
        assertThat(result.nextOffset()).isNull();
        assertThat(result.total()).isEqualTo(2L);
        then(getMemberUseCase).should().findAllByIds(Set.of(20L, 30L));
        then(getGisuUseCase).should().getByIds(Set.of(3L, 5L));
    }

    @Test
    @DisplayName("제외 대상과 이름 검색을 페이지 계산 전에 적용하고 안정적으로 정렬한다")
    void 검색은_제외와_키워드를_페이지_전에_적용한다() {
        // given
        List<ChallengerBasicInfo> challengers = List.of(
            challenger(101L, 10L, 1L, ChallengerStatus.GRADUATED),
            challenger(201L, 20L, 1L, ChallengerStatus.GRADUATED),
            challenger(301L, 30L, 1L, ChallengerStatus.ACTIVE),
            challenger(401L, 40L, 1L, ChallengerStatus.ACTIVE)
        );
        given(getChallengerUseCase.listLatestBasicPerMember()).willReturn(challengers);
        given(getMemberUseCase.findAllByIds(Set.of(10L, 20L, 30L))).willReturn(Map.of(
            10L, member(10L, "Alice Kim"),
            20L, member(20L, "alice Kim"),
            30L, member(30L, "Bob Kim")
        ));
        given(getGisuUseCase.getByIds(Set.of(1L))).willReturn(List.of(gisuInfo(1L, 6L)));

        // when
        ChallengerInvitationSearchResult firstPage = service().search(
            query("  ALICE  ", Set.of(40L), 0, 1)
        );
        ChallengerInvitationSearchResult secondPage = service().search(
            query("alice", Set.of(40L), 1, 1)
        );

        // then
        assertThat(firstPage.items()).extracting(ChallengerInvitationInfo::memberId).containsExactly(10L);
        assertThat(firstPage.nextOffset()).isEqualTo(1);
        assertThat(firstPage.total()).isEqualTo(2L);
        assertThat(secondPage.items()).extracting(ChallengerInvitationInfo::memberId).containsExactly(20L);
        assertThat(secondPage.nextOffset()).isNull();
        assertThat(secondPage.total()).isEqualTo(2L);
        then(getChallengerUseCase).should(times(2)).listLatestBasicPerMember();
        then(getMemberUseCase).should(times(2)).findAllByIds(Set.of(10L, 20L, 30L));
    }

    @Test
    @DisplayName("직접 초대는 회원의 최신 이력을 먼저 선택한 뒤 상태를 판정한다")
    void 직접_초대는_최신_챌린저_상태로_적격성을_판정한다() {
        // given
        Set<Long> memberIds = Set.of(10L, 20L, 30L, 40L, 50L);
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            10L, List.of(
                challenger(101L, 10L, 100L, ChallengerStatus.GRADUATED),
                challenger(105L, 10L, 5L, ChallengerStatus.WITHDRAWN)
            ),
            20L, List.of(
                challenger(201L, 20L, 200L, ChallengerStatus.ACTIVE),
                challenger(204L, 20L, 4L, ChallengerStatus.GRADUATED)
            ),
            30L, List.of(challenger(303L, 30L, 3L, ChallengerStatus.ACTIVE)),
            40L, List.of(challenger(402L, 40L, 2L, ChallengerStatus.EXPELLED))
        ));
        given(getMemberUseCase.findAllByIds(Set.of(20L, 30L))).willReturn(Map.of(
            20L, member(20L, "수료 회원"),
            30L, member(30L, "활동 회원")
        ));
        given(getGisuUseCase.getByIds(Set.of(2L, 3L, 4L, 5L, 100L, 200L))).willReturn(List.of(
            gisuInfo(100L, 6L),
            gisuInfo(200L, 7L),
            gisuInfo(2L, 7L),
            gisuInfo(3L, 8L),
            gisuInfo(4L, 9L),
            gisuInfo(5L, 10L)
        ));

        // when
        Map<Long, ChallengerInvitationInfo> result = service().batchGetEligibleChallengers(memberIds);

        // then
        assertThat(result).containsOnlyKeys(20L, 30L);
        assertThat(result.get(20L))
            .extracting(ChallengerInvitationInfo::challengerId, ChallengerInvitationInfo::generation)
            .containsExactly(204L, 9L);
        assertThat(result.get(30L))
            .extracting(ChallengerInvitationInfo::challengerId, ChallengerInvitationInfo::generation)
            .containsExactly(303L, 8L);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(memberIds);
        then(getMemberUseCase).should().findAllByIds(Set.of(20L, 30L));
        then(getGisuUseCase).should().getByIds(Set.of(2L, 3L, 4L, 5L, 100L, 200L));
    }

    @Test
    @DisplayName("빈 직접 초대 검증은 외부 Query UseCase를 호출하지 않는다")
    void 빈_직접_초대_검증은_외부_조회가_없다() {
        // when
        Map<Long, ChallengerInvitationInfo> result = service().batchGetEligibleChallengers(Set.of());

        // then
        assertThat(result).isEmpty();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    private ChallengerInvitationQueryService service() {
        return new ChallengerInvitationQueryService(
            getGisuUseCase,
            getChallengerUseCase,
            getMemberUseCase
        );
    }

    private SearchChallengerInvitationQuery query(
        String keyword,
        Set<Long> excludedMemberIds,
        int offset,
        int limit
    ) {
        return new SearchChallengerInvitationQuery(keyword, excludedMemberIds, offset, limit);
    }

    private GisuInfo gisuInfo(Long gisuId, Long generation) {
        return new GisuInfo(gisuId, generation, null, null, false);
    }

    private ChallengerBasicInfo challenger(
        Long challengerId,
        Long memberId,
        Long gisuId,
        ChallengerStatus status
    ) {
        return new ChallengerBasicInfo(challengerId, memberId, gisuId, ChallengerPart.WEB, status);
    }

    private MemberInfo member(Long memberId, String name) {
        return MemberInfo.builder()
            .id(memberId)
            .name(name)
            .build();
    }
}
