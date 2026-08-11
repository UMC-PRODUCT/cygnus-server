package com.umc.product.inhouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.domain.UmcProductMember;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 접근 정책")
class UmcProductAccessPolicyTest {

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;

    @Mock
    LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;

    @Mock
    UmcProductDateProvider umcProductDateProvider;

    @InjectMocks
    UmcProductAccessPolicy sut;

    @Test
    void 중앙_총괄단은_Leadership_조회_없이_조직을_관리할_수_있다() {
        given(getChallengerRoleUseCase.isCentralCoreInAnyGisu(1L)).willReturn(true);

        assertThat(sut.canManageUmcProduct(1L)).isTrue();

        then(loadUmcProductLeadershipPort).shouldHaveNoInteractions();
        then(loadUmcProductMemberAccountPort).shouldHaveNoInteractions();
        then(umcProductDateProvider).shouldHaveNoInteractions();
    }

    @Test
    void 오늘_유효한_Product_Leadership이_있으면_조직을_관리할_수_있다() {
        LocalDate today = LocalDate.of(2026, 7, 13);
        Set<UmcProductLeadershipRole> managerRoles = Set.of(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD
        );
        given(getChallengerRoleUseCase.isCentralCoreInAnyGisu(1L)).willReturn(false);
        given(loadUmcProductMemberAccountPort.findByMemberId(1L))
            .willReturn(Optional.of(account(30L, 1L)));
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductLeadershipPort.existsByUmcProductMemberIdAndRolesOnDate(
            30L,
            managerRoles,
            today
        )).willReturn(true);

        assertThat(sut.canManageUmcProduct(1L)).isTrue();
    }

    @Test
    void 오늘_유효한_Product_Leadership이_없으면_조직을_관리할_수_없다() {
        LocalDate today = LocalDate.of(2026, 7, 13);
        Set<UmcProductLeadershipRole> managerRoles = Set.of(
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD
        );
        given(getChallengerRoleUseCase.isCentralCoreInAnyGisu(1L)).willReturn(false);
        given(loadUmcProductMemberAccountPort.findByMemberId(1L))
            .willReturn(Optional.of(account(30L, 1L)));
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductLeadershipPort.existsByUmcProductMemberIdAndRolesOnDate(
            30L,
            managerRoles,
            today
        )).willReturn(false);

        assertThat(sut.canManageUmcProduct(1L)).isFalse();
    }

    @Test
    void 연동된_계정으로_요청하면_본인_프로필을_관리할_수_있다() {
        given(loadUmcProductMemberAccountPort.existsByUmcProductMemberIdAndMemberId(30L, 1L))
            .willReturn(true);

        assertThat(sut.canManageMemberProfile(1L, 30L)).isTrue();

        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
        then(loadUmcProductLeadershipPort).shouldHaveNoInteractions();
    }

    @Test
    void 연동되지_않은_계정이며_관리_권한도_없으면_프로필_관리가_거부된다() {
        given(loadUmcProductMemberAccountPort.existsByUmcProductMemberIdAndMemberId(30L, 1L))
            .willReturn(false);
        given(getChallengerRoleUseCase.isCentralCoreInAnyGisu(1L)).willReturn(false);
        given(loadUmcProductMemberAccountPort.findByMemberId(1L)).willReturn(Optional.empty());

        assertThat(sut.canManageMemberProfile(1L, 30L)).isFalse();
    }

    private UmcProductMemberAccount account(Long umcProductMemberId, Long memberId) {
        UmcProductMember member = UmcProductMember.create("테스트", "테스터", null, null, null);
        ReflectionTestUtils.setField(member, "id", umcProductMemberId);
        return UmcProductMemberAccount.create(member, memberId, UmcProductMemberAccountType.LINKED);
    }
}
