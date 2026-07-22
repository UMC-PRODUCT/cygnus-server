package com.umc.product.notification.adapter.out.external.fcm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;

@DisplayName("FCM 비활성화 adapter")
class NoopFcmAdapterTest {

    @Test
    @DisplayName("메시지 발송은 외부 호출 없이 빈 결과를 반환한다")
    void message_send_is_noop() {
        FcmSendRequest request = FcmSendRequest.of(
            List.of(FcmSendTarget.of(1L, "token")), "제목", "본문", Map.of(), null, null
        );

        assertThat(new NoopFcmMessageAdapter().send(request))
            .satisfies(result -> {
                assertThat(result.successCount()).isZero();
                assertThat(result.failureCount()).isZero();
                assertThat(result.invalidTokenIds()).isEmpty();
                assertThat(result.retryableTokenIds()).isEmpty();
            });
    }

    @Test
    @DisplayName("토큰 검증은 모든 입력 토큰을 유효한 것으로 간주한다")
    void token_validation_accepts_all_targets() {
        FcmTokenValidationRequest request = FcmTokenValidationRequest.of(List.of(
            FcmSendTarget.of(1L, "token-1"),
            FcmSendTarget.of(2L, "token-2")
        ));

        assertThat(new NoopFcmTokenValidationAdapter().validate(request))
            .satisfies(result -> {
                assertThat(result.successCount()).isEqualTo(2);
                assertThat(result.failureCount()).isZero();
                assertThat(result.validTokenIds()).containsExactly(1L, 2L);
                assertThat(result.invalidTokenIds()).isEmpty();
            });
    }
}
