package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.SearchMemberPort;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSearchService 검색 접근 범위")
class MemberSearchAccessScopeTest {

    private static final Long REQUESTER_MEMBER_ID = 1L;

    @Mock
    SearchMemberPort searchMemberPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @InjectMocks
    MemberSearchService sut;

    @Test
    @DisplayName("챌린저 기록이 없으면 회원 검색을 거부한다")
    void 챌린저_기록이_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of());

        assertThatThrownBy(() -> sut.searchBy(query, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 없으면 회원 검색 v2를 거부한다")
    void 챌린저_기록이_없으면_회원_검색_v2를_거부한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of());

        assertThatThrownBy(() -> sut.searchByV2(query, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 없으면 챌린저 검색 v2를 거부한다")
    void 챌린저_기록이_없으면_챌린저_검색_v2를_거부한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of());

        assertThatThrownBy(() -> sut.searchChallengersByV2(query, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 있으면 회원 검색을 전체 범위로 허용한다")
    void 챌린저_기록이_있으면_회원_검색을_전체_범위로_허용한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID)))
            .willReturn(challengersByRequester());
        given(searchMemberPort.search(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchBy(query, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isSameAs(query);
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 있으면 회원 검색 v2를 전체 범위로 허용한다")
    void 챌린저_기록이_있으면_회원_검색_v2를_전체_범위로_허용한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID)))
            .willReturn(challengersByRequester());
        given(searchMemberPort.searchMemberIds(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchByV2(query, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().searchMemberIds(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isSameAs(query);
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("운영진 기록만 있고 챌린저 기록이 없으면 회원 검색을 거부한다")
    void 운영진_기록만_있고_챌린저_기록이_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of());

        assertThatThrownBy(() -> sut.searchBy(query, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
    }

    private Map<Long, List<ChallengerBasicInfo>> challengersByRequester() {
        return Map.of(
            REQUESTER_MEMBER_ID,
            List.of(new ChallengerBasicInfo(
                101L,
                REQUESTER_MEMBER_ID,
                10L,
                ChallengerPart.SPRINGBOOT,
                ChallengerStatus.ACTIVE
            ))
        );
    }
}
