package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
class RecruitingGraphQlPermissionSupportTest {

    @Mock CheckPermissionUseCase checkPermissionUseCase;
    @Mock CurrentMemberProvider currentMemberProvider;
    @InjectMocks RecruitingGraphQlPermissionSupport sut;

    @Test
    @DisplayName("현재 회원과 명시 회원의 모집 type·season 권한을 동일한 permission 계약으로 검사한다")
    void delegateAllPermissionChecks() {
        given(currentMemberProvider.getRequiredCurrentMemberId()).willReturn(10L);
        given(checkPermissionUseCase.check(
            11L,
            ResourcePermission.of(ResourceType.RECRUITMENT, 100L, PermissionType.READ)
        )).willReturn(true);

        sut.assertRecruitmentTypePermission(PermissionType.WRITE);
        sut.assertRecruitmentTypePermission(11L, PermissionType.EDIT);
        sut.assertRecruitmentPermission(100L, PermissionType.READ);
        sut.assertRecruitmentPermission(11L, 100L, PermissionType.DELETE);

        assertThat(sut.hasRecruitmentPermission(11L, 100L, PermissionType.READ)).isTrue();
        then(checkPermissionUseCase).should().checkOrThrow(
            10L,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, PermissionType.WRITE)
        );
        then(checkPermissionUseCase).should().checkOrThrow(
            11L,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, PermissionType.EDIT)
        );
    }

    @Test
    @DisplayName("리소스 소속과 principal은 null일 때 fail-closed로 처리한다")
    void failClosedForMissingScopeAndPrincipal() {
        assertThatThrownBy(() -> sut.assertResourceBelongsToSeason(false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> sut.currentMemberId((MemberPrincipal) null))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(sut.currentMemberId(new MemberPrincipal(20L))).isEqualTo(20L);
        assertThat(sut.nullableCurrentMemberId((MemberPrincipal) null)).isNull();
        assertThat(sut.nullableCurrentMemberId(new MemberPrincipal(21L))).isEqualTo(21L);
        sut.assertResourceBelongsToSeason(true);
    }

    @Test
    @DisplayName("현재 회원 ID의 required·nullable 조회를 provider에 위임한다")
    void resolveCurrentMemberFromProvider() {
        given(currentMemberProvider.getRequiredCurrentMemberId()).willReturn(30L);
        given(currentMemberProvider.getNullableCurrentMemberId()).willReturn(null);

        assertThat(sut.currentMemberId()).isEqualTo(30L);
        assertThat(sut.nullableCurrentMemberId()).isNull();
    }
}
