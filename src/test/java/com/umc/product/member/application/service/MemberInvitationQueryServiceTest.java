package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberInvitationQuery;
import com.umc.product.member.application.port.out.SearchMemberInvitationPort;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidate;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidatePage;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 초대 대상 검색 서비스")
class MemberInvitationQueryServiceTest {

    @Mock
    SearchMemberInvitationPort searchMemberInvitationPort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Test
    @DisplayName("Challenger 이력이 없는 회원도 포함하고 이력이 있으면 상태와 무관하게 최신 정보를 조립한다")
    void 검색은_Challenger_이력과_무관하게_회원을_포함한다() {
        // given
        List<MemberInvitationCandidate> candidates = List.of(
            candidate(10L, "가무이력"),
            candidate(20L, "나탈퇴이력"),
            candidate(30L, "다활동이력"),
            candidate(40L, "라기수누락")
        );
        given(searchMemberInvitationPort.search(org.mockito.ArgumentMatchers.any()))
            .willReturn(new MemberInvitationCandidatePage(candidates, 4L));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(10L, 20L, 30L, 40L)))
            .willReturn(Map.of(
                20L, List.of(
                    challenger(201L, 20L, 100L, ChallengerStatus.GRADUATED),
                    challenger(205L, 20L, 5L, ChallengerStatus.WITHDRAWN)
                ),
                30L, List.of(challenger(303L, 30L, 3L, ChallengerStatus.ACTIVE)),
                40L, List.of(challenger(404L, 40L, 4L, ChallengerStatus.EXPELLED))
            ));
        given(getGisuUseCase.getByIds(Set.of(3L, 4L, 5L, 100L))).willReturn(List.of(
            gisuInfo(100L, 6L),
            gisuInfo(3L, 8L),
            gisuInfo(5L, 10L)
        ));

        // when
        MemberInvitationSearchResult result = service().search(query(null, Set.of(), 0, 10));

        // then
        assertThat(result.items())
            .extracting(
                MemberInvitationInfo::memberId,
                MemberInvitationInfo::challengerId,
                MemberInvitationInfo::generation
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(10L, null, null),
                org.assertj.core.groups.Tuple.tuple(20L, 205L, 10L),
                org.assertj.core.groups.Tuple.tuple(30L, 303L, 8L),
                org.assertj.core.groups.Tuple.tuple(40L, null, null)
            );
        assertThat(result.items()).extracting(MemberInvitationInfo::part)
            .containsExactly(null, ChallengerPart.WEB, ChallengerPart.WEB, null);
        assertThat(result.nextOffset()).isNull();
        assertThat(result.total()).isEqualTo(4L);
    }

    @Test
    @DisplayName("검색 조건과 offset 페이지 계산을 회원 검색 Port에 위임한다")
    void 검색은_조건과_페이지를_회원_검색_Port에_위임한다() {
        // given
        given(searchMemberInvitationPort.search(org.mockito.ArgumentMatchers.any()))
            .willReturn(new MemberInvitationCandidatePage(
                List.of(candidate(30L, "새 회원")),
                7L
            ));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(30L))).willReturn(Map.of());

        // when
        MemberInvitationSearchResult result = service().search(
            query("  새  ", Set.of(10L, 20L), 2, 1)
        );

        // then
        assertThat(result.items()).extracting(MemberInvitationInfo::memberId).containsExactly(30L);
        assertThat(result.nextOffset()).isEqualTo(3);
        assertThat(result.total()).isEqualTo(7L);
        ArgumentCaptor<SearchMemberInvitationCondition> captor =
            ArgumentCaptor.forClass(SearchMemberInvitationCondition.class);
        then(searchMemberInvitationPort).should().search(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("새");
        assertThat(captor.getValue().excludedMemberIds()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(captor.getValue().offset()).isEqualTo(2);
        assertThat(captor.getValue().limit()).isEqualTo(1);
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 페이지도 전체 개수를 보존하고 Challenger 조회를 생략한다")
    void 빈_페이지는_전체_개수를_보존한다() {
        // given
        given(searchMemberInvitationPort.search(org.mockito.ArgumentMatchers.any()))
            .willReturn(new MemberInvitationCandidatePage(List.of(), 3L));

        // when
        MemberInvitationSearchResult result = service().search(query(null, Set.of(), 10, 10));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.nextOffset()).isNull();
        assertThat(result.total()).isEqualTo(3L);
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("직접 초대는 Challenger 이력 없이 활성 회원 존재 여부만 검증한다")
    void 직접_초대는_활성_회원만_허용한다() {
        // given
        Set<Long> memberIds = Set.of(10L, 20L, 30L, 40L);
        given(searchMemberInvitationPort.findActiveMemberIds(memberIds)).willReturn(Set.of(10L));

        // when
        Set<Long> result = service().batchGetInvitableMemberIds(memberIds);

        // then
        assertThat(result).containsExactly(10L);
        then(searchMemberInvitationPort).should().findActiveMemberIds(memberIds);
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 직접 초대 검증은 외부 조회를 호출하지 않는다")
    void 빈_직접_초대_검증은_외부_조회가_없다() {
        // when
        Set<Long> result = service().batchGetInvitableMemberIds(Set.of());

        // then
        assertThat(result).isEmpty();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
        then(searchMemberInvitationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("직접 초대 회원 ID에 null이나 양수가 아닌 값이 있으면 조회 전에 거부한다")
    void 직접_초대는_잘못된_회원_ID를_거부한다() {
        assertThatThrownBy(() -> service().batchGetInvitableMemberIds(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service().batchGetInvitableMemberIds(Set.of(0L)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service().batchGetInvitableMemberIds(Set.of(-1L)))
            .isInstanceOf(IllegalArgumentException.class);

        then(searchMemberInvitationPort).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    private MemberInvitationQueryService service() {
        return new MemberInvitationQueryService(
            searchMemberInvitationPort,
            getGisuUseCase,
            getChallengerUseCase
        );
    }

    private SearchMemberInvitationQuery query(
        String keyword,
        Set<Long> excludedMemberIds,
        int offset,
        int limit
    ) {
        return new SearchMemberInvitationQuery(keyword, excludedMemberIds, offset, limit);
    }

    private MemberInvitationCandidate candidate(Long memberId, String name) {
        return new MemberInvitationCandidate(memberId, name);
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

}
