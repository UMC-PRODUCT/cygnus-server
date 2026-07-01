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
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.OrganizationType;
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
    @DisplayName("챌린저 기록과 운영진 기록이 모두 없으면 회원 검색을 거부한다")
    void 챌린저_기록과_운영진_기록이_모두_없으면_회원_검색을_거부한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerRoleUseCase.findAllByMemberId(REQUESTER_MEMBER_ID)).willReturn(List.of());
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of());

        assertThatThrownBy(() -> sut.searchBy(query, pageable))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED);

        then(searchMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("챌린저 기록이 있으면 해당 기수들로 검색 범위를 제한한다")
    void 챌린저_기록이_있으면_해당_기수들로_검색_범위를_제한한다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerRoleUseCase.findAllByMemberId(REQUESTER_MEMBER_ID)).willReturn(List.of());
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(REQUESTER_MEMBER_ID))).willReturn(Map.of(
            REQUESTER_MEMBER_ID,
            List.of(
                challenger(101L, 10L),
                challenger(102L, 20L)
            )
        ));
        given(searchMemberPort.search(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchBy(query, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue().accessScope().allowedGisuIds())
            .containsExactlyInAnyOrder(10L, 20L);
        assertThat(queryCaptor.getValue().accessScope().allowedSchoolIds()).isEmpty();
    }

    @Test
    @DisplayName("교내 회장 또는 부회장 기록이 있으면 본인 학교로 검색 범위를 제한하고 기수는 제한하지 않는다")
    void 교내_회장_또는_부회장_기록이_있으면_본인_학교로_검색_범위를_제한하고_기수는_제한하지_않는다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerRoleUseCase.findAllByMemberId(REQUESTER_MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.SCHOOL_PRESIDENT, 30L),
            role(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, 40L)
        ));
        given(searchMemberPort.search(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchBy(query, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue().accessScope().allowedSchoolIds())
            .containsExactlyInAnyOrder(30L, 40L);
        assertThat(queryCaptor.getValue().accessScope().allowedGisuIds()).isEmpty();
        then(getChallengerUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중앙운영사무국 총괄단 또는 SUPER_ADMIN 기록이 있으면 검색 범위를 제한하지 않는다")
    void 중앙운영사무국_총괄단_또는_SUPER_ADMIN_기록이_있으면_검색_범위를_제한하지_않는다() {
        SearchMemberQuery query = SearchMemberQuery.of(REQUESTER_MEMBER_ID, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        given(getChallengerRoleUseCase.findAllByMemberId(REQUESTER_MEMBER_ID)).willReturn(List.of(
            role(ChallengerRoleType.CENTRAL_VICE_PRESIDENT, null),
            role(ChallengerRoleType.SUPER_ADMIN, null)
        ));
        given(searchMemberPort.search(any(), any())).willReturn(new PageImpl<>(List.of(), pageable, 0));

        sut.searchBy(query, pageable);

        ArgumentCaptor<SearchMemberQuery> queryCaptor = ArgumentCaptor.forClass(SearchMemberQuery.class);
        then(searchMemberPort).should().search(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue().accessScope().unrestricted()).isTrue();
        then(getChallengerUseCase).shouldHaveNoInteractions();
    }

    private ChallengerBasicInfo challenger(Long challengerId, Long gisuId) {
        return new ChallengerBasicInfo(
            challengerId,
            REQUESTER_MEMBER_ID,
            gisuId,
            ChallengerPart.SPRINGBOOT,
            ChallengerStatus.ACTIVE
        );
    }

    private ChallengerRoleInfo role(ChallengerRoleType roleType, Long organizationId) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(100L)
            .roleType(roleType)
            .organizationType(organizationId == null ? OrganizationType.CENTRAL : OrganizationType.SCHOOL)
            .organizationId(organizationId)
            .gisuId(10L)
            .build();
    }
}
