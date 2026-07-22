package com.umc.product.maintenance.adapter.out.bypass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;

@DisplayName("점검 우회 정책")
class ChallengerRoleBasedBypassPolicyTest {

    @Test
    @DisplayName("null 회원은 권한 조회 없이 우회하지 않는다")
    void null_회원은_우회하지_않는다() {
        GetChallengerRoleUseCase roles = mock(GetChallengerRoleUseCase.class);
        ChallengerRoleBasedBypassPolicy policy = new ChallengerRoleBasedBypassPolicy(roles);

        assertThat(policy.shouldBypass(null)).isFalse();
        then(roles).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUPER_ADMIN 판정 결과를 그대로 반환한다")
    void SUPER_ADMIN만_우회한다() {
        GetChallengerRoleUseCase roles = mock(GetChallengerRoleUseCase.class);
        ChallengerRoleBasedBypassPolicy policy = new ChallengerRoleBasedBypassPolicy(roles);
        given(roles.isSuperAdmin(1L)).willReturn(true);
        given(roles.isSuperAdmin(2L)).willReturn(false);

        assertThat(policy.shouldBypass(1L)).isTrue();
        assertThat(policy.shouldBypass(2L)).isFalse();
    }
}
