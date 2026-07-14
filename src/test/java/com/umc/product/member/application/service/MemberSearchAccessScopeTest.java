package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.CheckChallengerHistoryUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchService 검색 접근 정책")
class MemberSearchAccessScopeTest {

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

    @Mock
    MemberSearchAccessScopeResolver memberSearchAccessScopeResolver;

    @InjectMocks
    MemberSearchService sut;

    @Test
    @DisplayName("GraphQL 검색 범위가 거부되면 포트를 호출하기 전에 예외를 던진다")
    void graphql_검색_범위가_거부되면_포트_호출_전에_예외를_던진다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        given(memberSearchAccessScopeResolver.resolve(REQUESTER_MEMBER_ID))
            .willReturn(MemberSearchAccessScope.denyAll());

        assertThatThrownBy(() -> sut.searchByV2ForGraphQl(query, REQUESTER_MEMBER_ID, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(checkChallengerHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GraphQL 제한 검색은 학교와 기수 범위를 범위 포트에 전달한다")
    void graphql_제한_검색은_scope를_범위_포트에_전달한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        MemberSearchAccessScope scope = MemberSearchAccessScope.restrictedTo(Set.of(7L), Set.of(3L, 4L));
        given(memberSearchAccessScopeResolver.resolve(REQUESTER_MEMBER_ID)).willReturn(scope);
        given(searchMemberPort.searchMemberIds(query, scope, pageable)).willReturn(Page.empty(pageable));

        var result = sut.searchByV2ForGraphQl(query, REQUESTER_MEMBER_ID, pageable);

        assertThat(result.page()).isEmpty();
        then(searchMemberPort).should().searchMemberIds(query, scope, pageable);
        then(searchMemberPort).shouldHaveNoMoreInteractions();
        then(checkChallengerHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GraphQL 무제한 검색도 범위 포트에 무제한 범위를 전달한다")
    void graphql_무제한_검색은_무제한_scope를_범위_포트에_전달한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        MemberSearchAccessScope scope = MemberSearchAccessScope.allowAll();
        given(memberSearchAccessScopeResolver.resolve(REQUESTER_MEMBER_ID)).willReturn(scope);
        given(searchMemberPort.searchMemberIds(query, scope, pageable)).willReturn(Page.empty(pageable));

        var result = sut.searchByV2ForGraphQl(query, REQUESTER_MEMBER_ID, pageable);

        assertThat(result.page()).isEmpty();
        then(searchMemberPort).should().searchMemberIds(query, scope, pageable);
        then(searchMemberPort).shouldHaveNoMoreInteractions();
        then(checkChallengerHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 없으면 회원 검색을 거부한다")
    void 챌린저_기록이_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> sut.searchBy(query, REQUESTER_MEMBER_ID, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 없으면 회원 검색 v2를 거부한다")
    void 챌린저_기록이_없으면_회원_검색_v2를_거부한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> sut.searchByV2(query, REQUESTER_MEMBER_ID, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 없으면 챌린저 검색 v2를 거부한다")
    void 챌린저_기록이_없으면_챌린저_검색_v2를_거부한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> sut.searchChallengersByV2(query, REQUESTER_MEMBER_ID, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 있으면 회원 검색을 전체 범위로 허용한다")
    void 챌린저_기록이_있으면_회원_검색을_전체_범위로_허용한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(true);
        given(searchMemberPort.search(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchBy(query, REQUESTER_MEMBER_ID, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isSameAs(query);
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 있으면 회원 검색 v2를 전체 범위로 허용한다")
    void 챌린저_기록이_있으면_회원_검색_v2를_전체_범위로_허용한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(true);
        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchByV2(query, REQUESTER_MEMBER_ID, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().searchMemberIds(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isSameAs(query);
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("운영진 기록만 있고 챌린저 기록이 없으면 회원 검색을 거부한다")
    void 운영진_기록만_있고_챌린저_기록이_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(checkChallengerHistoryUseCase.hasChallengerHistory(REQUESTER_MEMBER_ID)).willReturn(false);

        assertThatThrownBy(() -> sut.searchBy(query, REQUESTER_MEMBER_ID, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청자 ID가 없으면 회원 검색을 거부한다")
    void 요청자_ID가_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = new SearchMemberQuery(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> sut.searchBy(query, null, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(checkChallengerHistoryUseCase).should().hasChallengerHistory(null);
        then(searchMemberPort).shouldHaveNoInteractions();
    }
}
