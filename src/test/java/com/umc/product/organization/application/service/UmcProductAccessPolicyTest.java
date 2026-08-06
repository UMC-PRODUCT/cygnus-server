package com.umc.product.organization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 접근 정책")
class UmcProductAccessPolicyTest {

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;

    @Mock
    UmcProductDateProvider umcProductDateProvider;

    @InjectMocks
    UmcProductAccessPolicy sut;

    @Test
    void 중앙_총괄단은_Leadership_조회_없이_조직을_관리할_수_있다() {
        given(getChallengerRoleUseCase.isCentralCoreInAnyGisu(1L)).willReturn(true);

        assertThat(sut.canManageUmcProduct(1L)).isTrue();

        then(loadUmcProductLeadershipPort).shouldHaveNoInteractions();
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
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductLeadershipPort.existsByMemberIdAndRolesOnDate(
            1L,
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
        given(umcProductDateProvider.today()).willReturn(today);
        given(loadUmcProductLeadershipPort.existsByMemberIdAndRolesOnDate(
            1L,
            managerRoles,
            today
        )).willReturn(false);

        assertThat(sut.canManageUmcProduct(1L)).isFalse();
    }

    @Test
    void 본인은_Leadership이_없어도_자신의_프로필을_관리할_수_있다() {
        assertThat(sut.canManageMemberProfile(1L, 1L)).isTrue();

        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
        then(loadUmcProductLeadershipPort).shouldHaveNoInteractions();
    }
}
