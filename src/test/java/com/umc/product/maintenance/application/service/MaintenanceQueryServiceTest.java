package com.umc.product.maintenance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.maintenance.application.port.out.LoadMaintenanceWindowPort;
import com.umc.product.maintenance.domain.MaintenanceScope;
import com.umc.product.maintenance.domain.MaintenanceWindow;
import com.umc.product.maintenance.exception.MaintenanceDomainException;
import com.umc.product.maintenance.exception.MaintenanceErrorCode;

@ExtendWith(MockitoExtension.class)
class MaintenanceQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");

    @Mock
    LoadMaintenanceWindowPort loadPort;

    MaintenanceQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new MaintenanceQueryService(loadPort, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        void 활성도_예약도_없으면_inMaintenance_false_와_null() {
            given(loadPort.findActiveAt(NOW)).willReturn(Optional.empty());
            given(loadPort.findNextUpcoming(NOW)).willReturn(Optional.empty());

            var status = sut.getStatus();

            assertThat(status.inMaintenance()).isFalse();
            assertThat(status.current()).isNull();
            assertThat(status.upcoming()).isNull();
        }

        @Test
        void 활성_윈도우가_있으면_current_와_inMaintenance_true() {
            MaintenanceWindow active = MaintenanceWindow.of(
                MaintenanceScope.FULL, null,
                NOW.minusSeconds(30),
                NOW.plus(Duration.ofHours(1)),
                "t", "m", 1L, NOW
            );
            given(loadPort.findActiveAt(NOW)).willReturn(Optional.of(active));
            given(loadPort.findNextUpcoming(NOW)).willReturn(Optional.empty());

            var status = sut.getStatus();

            assertThat(status.inMaintenance()).isTrue();
            assertThat(status.current()).isNotNull();
        }

        @Test
        void 활성_없고_예약만_있으면_upcoming_채워짐() {
            MaintenanceWindow upcoming = MaintenanceWindow.of(
                MaintenanceScope.FULL, null,
                NOW.plus(Duration.ofMinutes(30)),
                NOW.plus(Duration.ofHours(2)),
                "t", "m", 1L, NOW
            );
            given(loadPort.findActiveAt(NOW)).willReturn(Optional.empty());
            given(loadPort.findNextUpcoming(NOW)).willReturn(Optional.of(upcoming));

            var status = sut.getStatus();

            assertThat(status.inMaintenance()).isFalse();
            assertThat(status.current()).isNull();
            assertThat(status.upcoming()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        void 존재하는_윈도우는_도메인_집합과_함께_반환한다() {
            MaintenanceWindow window = MaintenanceWindow.of(
                MaintenanceScope.PER_DOMAIN,
                EnumSet.of(com.umc.product.maintenance.domain.MaintenanceDomain.NOTICE),
                NOW,
                NOW.plus(Duration.ofHours(1)),
                "공지 점검",
                "점검 중",
                1L,
                NOW
            );
            given(loadPort.findById(7L)).willReturn(Optional.of(window));

            var result = sut.getById(7L);

            assertThat(result.targetDomains())
                .containsExactly(com.umc.product.maintenance.domain.MaintenanceDomain.NOTICE);
        }

        @Test
        void 없는_id_면_예외() {
            given(loadPort.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getById(99L))
                .isInstanceOf(MaintenanceDomainException.class)
                .hasFieldOrPropertyWithValue("baseCode", MaintenanceErrorCode.MAINTENANCE_WINDOW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        void 빈_목록도_정상_반환() {
            given(loadPort.findAllOrderByCreatedAtDesc()).willReturn(List.of());

            assertThat(sut.listAll()).isEmpty();
        }

        @Test
        void 목록의_윈도우를_응답으로_변환한다() {
            MaintenanceWindow window = MaintenanceWindow.of(
                MaintenanceScope.FULL,
                null,
                NOW,
                NOW.plus(Duration.ofHours(1)),
                "전체 점검",
                "점검 중",
                1L,
                NOW
            );
            given(loadPort.findAllOrderByCreatedAtDesc()).willReturn(List.of(window));

            assertThat(sut.listAll()).singleElement()
                .satisfies(info -> assertThat(info.title()).isEqualTo("전체 점검"));
        }
    }
}
