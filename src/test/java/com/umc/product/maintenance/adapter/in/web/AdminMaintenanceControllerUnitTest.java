package com.umc.product.maintenance.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.maintenance.application.port.in.command.ManageMaintenanceUseCase;
import com.umc.product.maintenance.application.port.in.query.GetMaintenanceStatusUseCase;
import com.umc.product.maintenance.application.port.in.query.dto.MaintenanceWindowInfo;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.maintenance.exception.MaintenanceDomainException;

@DisplayName("AdminMaintenanceController 단건 조회")
class AdminMaintenanceControllerUnitTest {

    @Test
    @DisplayName("SUPER_ADMIN은 점검 윈도우를 ID로 조회한다")
    void SUPER_ADMIN은_단건_조회한다() {
        GetMaintenanceStatusUseCase query = mock(GetMaintenanceStatusUseCase.class);
        MaintenanceBypassPolicy bypassPolicy = mock(MaintenanceBypassPolicy.class);
        AdminMaintenanceController controller = new AdminMaintenanceController(
            mock(ManageMaintenanceUseCase.class),
            query,
            bypassPolicy
        );
        MemberPrincipal principal = MemberPrincipal.builder().memberId(1L).build();
        given(bypassPolicy.shouldBypass(1L)).willReturn(true);
        given(query.getById(7L)).willReturn(info());

        assertThat(controller.getOne(principal, 7L).id()).isEqualTo(7L);
    }

    @Test
    @DisplayName("비인증 및 일반 회원은 단건 조회를 할 수 없다")
    void 권한_없는_단건_조회를_거부한다() {
        MaintenanceBypassPolicy bypassPolicy = mock(MaintenanceBypassPolicy.class);
        AdminMaintenanceController controller = new AdminMaintenanceController(
            mock(ManageMaintenanceUseCase.class),
            mock(GetMaintenanceStatusUseCase.class),
            bypassPolicy
        );
        MemberPrincipal principal = MemberPrincipal.builder().memberId(2L).build();
        given(bypassPolicy.shouldBypass(2L)).willReturn(false);

        assertThatThrownBy(() -> controller.getOne(null, 7L))
            .isInstanceOf(MaintenanceDomainException.class);
        assertThatThrownBy(() -> controller.getOne(principal, 7L))
            .isInstanceOf(MaintenanceDomainException.class);
    }

    private MaintenanceWindowInfo info() {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        return new MaintenanceWindowInfo(
            7L,
            MaintenanceScope.FULL,
            Set.of(),
            now,
            now.plusSeconds(3600),
            "전체 점검",
            "점검 중",
            null,
            null,
            1L,
            now
        );
    }
}
