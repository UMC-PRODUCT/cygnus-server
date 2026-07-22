package com.umc.product.maintenance.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaintenanceDomainException")
class MaintenanceDomainExceptionTest {

    @Test
    @DisplayName("운영자가 이해할 수 있는 상세 메시지를 보존한다")
    void 상세_메시지를_보존한다() {
        MaintenanceDomainException exception = new MaintenanceDomainException(
            MaintenanceErrorCode.ALREADY_ENDED,
            "이미 종료된 점검입니다"
        );

        assertThat(exception.getMessage()).isEqualTo("이미 종료된 점검입니다");
        assertThat(exception.getBaseCode()).isEqualTo(MaintenanceErrorCode.ALREADY_ENDED);
    }
}
