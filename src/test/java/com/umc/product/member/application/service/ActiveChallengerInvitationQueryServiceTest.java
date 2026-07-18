package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.umc.product.member.application.port.in.query.dto.ActiveChallengerInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.ActiveChallengerInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchActiveChallengerInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("활성 챌린저 초대 대상 검색 서비스")
class ActiveChallengerInvitationQueryServiceTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Test
    @DisplayName("현재 활성 기수가 없으면 빈 결과를 반환한다")
    void 현재_활성_기수가_없으면_빈_결과를_반환한다() {
        // given
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.empty());
        ActiveChallengerInvitationQueryService sut = service();

        // when
        ActiveChallengerInvitationSearchResult result = sut.search(query(5L, null, Set.of(), 0, 10));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.nextOffset()).isNull();
        assertThat(result.total()).isZero();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 기수가 현재 활성 기수와 다르면 빈 결과를 반환한다")
    void 요청_기수가_현재_활성_기수와_다르면_빈_결과를_반환한다() {
        // given
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisuInfo(6L, 10L, true)));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        ActiveChallengerInvitationSearchResult result = sut.search(query(5L, null, Set.of(), 0, 10));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 기수가 비활성 상태이면 빈 결과를 반환한다")
    void 요청_기수가_비활성_상태이면_빈_결과를_반환한다() {
        // given
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisuInfo(5L, 10L, false)));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        ActiveChallengerInvitationSearchResult result = sut.search(query(5L, null, Set.of(), 0, 10));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName(
        "공개 Query UseCase 후보를 일괄 조회한 뒤 제외·정렬·페이지·카운트를 적용한다"
    )
    void 공개_Query_UseCase_후보를_일괄_조회한_뒤_페이지한다() {
        // given
        GisuInfo activeGisu = gisuInfo(5L, 10L, true);
        ChallengerBasicInfo excluded = challenger(1_001L, 10L, ChallengerPart.PLAN, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo lower = challenger(2_002L, 20L, ChallengerPart.WEB, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo higher = challenger(3_003L, 30L, ChallengerPart.DESIGN, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo last = challenger(4_004L, 40L, ChallengerPart.IOS, ChallengerStatus.ACTIVE);
        ChallengerBasicInfo graduated = challenger(5_005L, 50L, ChallengerPart.ANDROID, ChallengerStatus.GRADUATED);
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(activeGisu));
        given(getChallengerUseCase.listBasicByGisuId(5L))
            .willReturn(List.of(excluded, higher, graduated, last, lower));
        given(getMemberUseCase.findAllByIds(Set.of(20L, 30L, 40L))).willReturn(Map.of(
            20L, member(20L, "나나다"),
            30L, member(30L, "나나다"),
            40L, member(40L, "다나다")
        ));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        ActiveChallengerInvitationSearchResult firstPage = sut.search(
            query(5L, null, Set.of(10L), 0, 2)
        );
        ActiveChallengerInvitationSearchResult secondPage = sut.search(
            query(5L, null, Set.of(10L), 2, 2)
        );

        // then
        assertThat(firstPage.items())
            .extracting(
                ActiveChallengerInvitationInfo::memberId,
                ActiveChallengerInvitationInfo::challengerId,
                ActiveChallengerInvitationInfo::generation
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(20L, 2_002L, 10L),
                org.assertj.core.groups.Tuple.tuple(30L, 3_003L, 10L)
            );
        assertThat(firstPage.total()).isEqualTo(3);
        assertThat(firstPage.nextOffset()).isEqualTo(2);
        assertThat(secondPage.items())
            .extracting(ActiveChallengerInvitationInfo::memberId)
            .containsExactly(40L);
        assertThat(secondPage.nextOffset()).isNull();
        then(getChallengerUseCase).should(times(2)).listBasicByGisuId(5L);
        then(getMemberUseCase).should(times(2)).findAllByIds(Set.of(20L, 30L, 40L));
    }

    @Test
    @DisplayName("키워드는 Member Query UseCase가 반환한 이름에 대소문자 구분 없이 적용한다")
    void 키워드는_회원_이름에_적용한다() {
        // given
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisuInfo(5L, 10L, true)));
        given(getChallengerUseCase.listBasicByGisuId(5L))
            .willReturn(List.of(
                challenger(100L, 10L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
                challenger(200L, 20L, ChallengerPart.WEB, ChallengerStatus.ACTIVE)
            ));
        given(getMemberUseCase.findAllByIds(Set.of(10L, 20L))).willReturn(Map.of(
            10L, member(10L, "Alice Kim"),
            20L, member(20L, "Bob Kim")
        ));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        ActiveChallengerInvitationSearchResult result = sut.search(
            query(5L, "  alice  ", Set.of(), 0, 10)
        );

        // then
        assertThat(result.items()).extracting(ActiveChallengerInvitationInfo::memberId).containsExactly(10L);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("일괄 적격성은 Challenger와 Member 공개 Query UseCase를 각각 한 번씩 사용한다")
    void 일괄_적격성은_공개_Query_UseCase를_조합한다() {
        // given
        Set<Long> memberIds = Set.of(10L, 20L, 30L);
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisuInfo(5L, 10L, true)));
        given(getChallengerUseCase.listBasicByMemberIdsAndGisuId(memberIds, 5L)).willReturn(List.of(
            new ChallengerBasicInfo(1_010L, 10L, 5L, ChallengerPart.WEB, ChallengerStatus.ACTIVE),
            new ChallengerBasicInfo(2_020L, 20L, 5L, ChallengerPart.WEB, ChallengerStatus.GRADUATED)
        ));
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(Map.of(
            10L, member(10L, "적격 회원"),
            20L, member(20L, "수료 회원")
        ));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        Map<Long, ActiveChallengerInvitationInfo> result = sut.batchGetEligibleActiveChallengers(5L, memberIds);

        // then
        assertThat(result).containsOnlyKeys(10L);
        assertThat(result.get(10L))
            .extracting(
                ActiveChallengerInvitationInfo::challengerId,
                ActiveChallengerInvitationInfo::generation
            )
            .containsExactly(1_010L, 10L);
        then(getChallengerUseCase).should(times(1)).listBasicByMemberIdsAndGisuId(memberIds, 5L);
        then(getMemberUseCase).should(times(1)).findAllByIds(memberIds);
    }

    @Test
    @DisplayName(
        "일괄 적격성은 현재 활성 기수가 아니면 공개 Query UseCase를 호출하지 않는다"
    )
    void 일괄_적격성은_비활성_기수를_거절한다() {
        // given
        given(getGisuUseCase.findActiveGisu()).willReturn(Optional.of(gisuInfo(6L, 10L, true)));
        ActiveChallengerInvitationQueryService sut = service();

        // when
        Map<Long, ActiveChallengerInvitationInfo> result = sut.batchGetEligibleActiveChallengers(5L, Set.of(10L));

        // then
        assertThat(result).isEmpty();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    private ActiveChallengerInvitationQueryService service() {
        return new ActiveChallengerInvitationQueryService(
            getGisuUseCase,
            getChallengerUseCase,
            getMemberUseCase
        );
    }

    private SearchActiveChallengerInvitationQuery query(
        Long gisuId,
        String keyword,
        Set<Long> excludedMemberIds,
        int offset,
        int limit
    ) {
        return new SearchActiveChallengerInvitationQuery(gisuId, keyword, excludedMemberIds, offset, limit);
    }

    private GisuInfo gisuInfo(Long gisuId, Long generation, boolean active) {
        return new GisuInfo(gisuId, generation, null, null, active);
    }

    private ChallengerBasicInfo challenger(
        Long challengerId,
        Long memberId,
        ChallengerPart part,
        ChallengerStatus status
    ) {
        return new ChallengerBasicInfo(challengerId, memberId, 5L, part, status);
    }

    private MemberInfo member(Long memberId, String name) {
        return MemberInfo.builder()
            .id(memberId)
            .name(name)
            .build();
    }
}
