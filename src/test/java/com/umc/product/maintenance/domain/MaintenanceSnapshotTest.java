package com.umc.product.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("MaintenanceSnapshot 차단 정책")
class MaintenanceSnapshotTest {

    @Test
    @DisplayName("비활성 스냅샷은 어떤 요청도 차단하지 않는다")
    void 비활성_스냅샷은_차단하지_않는다() {
        assertThat(MaintenanceSnapshot.none().blocks("/api/v1/notices/1")).isFalse();
    }

    @Test
    @DisplayName("전체 점검은 모든 요청을 차단한다")
    void 전체_점검은_모든_요청을_차단한다() {
        MaintenanceSnapshot snapshot = new MaintenanceSnapshot(
            true,
            1L,
            MaintenanceScope.FULL,
            EnumSet.noneOf(MaintenanceDomain.class),
            Instant.parse("2026-07-22T00:00:00Z"),
            Instant.parse("2026-07-22T01:00:00Z"),
            "전체 점검",
            "잠시 기다려주세요"
        );

        assertThat(snapshot.blocks("/unknown")).isTrue();
    }

    @Test
    @DisplayName("부분 점검은 선택한 도메인만 차단하고 알 수 없는 URI는 허용한다")
    void 부분_점검은_선택한_도메인만_차단한다() {
        MaintenanceSnapshot snapshot = new MaintenanceSnapshot(
            true,
            1L,
            MaintenanceScope.PER_DOMAIN,
            EnumSet.of(MaintenanceDomain.NOTICE),
            Instant.parse("2026-07-22T00:00:00Z"),
            Instant.parse("2026-07-22T01:00:00Z"),
            "공지 점검",
            "공지 기능 점검 중"
        );

        assertThat(snapshot.blocks("/api/v1/notices/1")).isTrue();
        assertThat(snapshot.blocks("/api/v1/projects/1")).isFalse();
        assertThat(snapshot.blocks("/unknown")).isFalse();
    }

    @Test
    @DisplayName("도메인 집합이 있는 윈도우를 불변 스냅샷으로 복사한다")
    void 윈도우를_스냅샷으로_복사한다() {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        MaintenanceWindow window = MaintenanceWindow.of(
            MaintenanceScope.PER_DOMAIN,
            EnumSet.of(MaintenanceDomain.NOTICE),
            now,
            now.plusSeconds(3600),
            "공지 점검",
            "공지 기능 점검 중",
            99L,
            now
        );
        ReflectionTestUtils.setField(window, "id", 10L);

        MaintenanceSnapshot snapshot = MaintenanceSnapshot.from(window);

        assertThat(snapshot.active()).isTrue();
        assertThat(snapshot.activeWindowId()).isEqualTo(10L);
        assertThat(snapshot.targetDomains()).containsExactly(MaintenanceDomain.NOTICE);
    }
}
