package com.umc.product.maintenance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.maintenance.application.port.out.LoadMaintenanceWindowPort;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.maintenance.domain.MaintenanceWindow;

@DisplayName("MaintenanceStateHolder 스냅샷 갱신")
class MaintenanceStateHolderTest {

    private static final Instant NOW = Instant.parse("2026-07-22T00:00:00Z");

    @Test
    @DisplayName("조회 실패 시 마지막 정상 스냅샷을 유지한다")
    void 조회_실패_시_마지막_스냅샷을_유지한다() {
        LoadMaintenanceWindowPort loadPort = mock(LoadMaintenanceWindowPort.class);
        MaintenanceWindow active = MaintenanceWindow.of(
            MaintenanceScope.FULL,
            null,
            NOW,
            NOW.plusSeconds(3600),
            "전체 점검",
            "점검 중",
            1L,
            NOW
        );
        given(loadPort.findActiveAt(NOW))
            .willReturn(Optional.of(active))
            .willThrow(new IllegalStateException("database unavailable"));
        MaintenanceStateHolder holder = new MaintenanceStateHolder(
            loadPort,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        holder.refresh();
        assertThat(holder.current().active()).isTrue();

        holder.refresh();
        assertThat(holder.current().active()).isTrue();
        assertThat(holder.current().title()).isEqualTo("전체 점검");
    }
}
