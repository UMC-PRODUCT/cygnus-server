package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.domain.FcmToken;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("FCM 배치 발송 이벤트 리스너")
class FcmSendBatchRequestedEventListenerTest {

    @Test
    @DisplayName("UNREGISTERED 결과를 받은 토큰을 비활성화한다")
    void invalid_token_비활성화() {
        // given
        FcmToken active = token(1L, 10L, "token-1");
        FcmToken invalid = token(2L, 20L, "token-2");
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(active, invalid));
        FakeSaveFcmPort saveFcmPort = new FakeSaveFcmPort();
        FakeSendFcmMessagePort sendFcmMessagePort = new FakeSendFcmMessagePort(
            FcmSendResult.of(1, 1, List.of(2L))
        );
        FcmSendBatchRequestedEventListener listener = new FcmSendBatchRequestedEventListener(
            new FcmProperties(true),
            loadFcmPort,
            saveFcmPort,
            sendFcmMessagePort,
            new OperationalMetrics(new SimpleMeterRegistry())
        );
        FcmSendBatchRequestedEvent event = new FcmSendBatchRequestedEvent(
            null,
            null,
            UUID.randomUUID(),
            List.of(1L, 2L),
            "제목",
            "본문",
            Map.of(),
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        assertThat(sendFcmMessagePort.lastRequest.targets()).hasSize(2);
        assertThat(invalid.isActive()).isFalse();
        assertThat(active.isActive()).isTrue();
        assertThat(saveFcmPort.saved).containsExactly(invalid);
    }

    private FcmToken token(Long id, Long memberId, String value) {
        FcmToken token = FcmToken.create(memberId, value);
        ReflectionTestUtils.setField(token, "id", id);
        return token;
    }

    private static class FakeLoadFcmPort implements LoadFcmPort {

        private final List<FcmToken> tokens;

        private FakeLoadFcmPort(List<FcmToken> tokens) {
            this.tokens = tokens;
        }

        @Override
        public Optional<FcmToken> findByMemberIdAndToken(Long memberId, String fcmToken) {
            return Optional.empty();
        }

        @Override
        public List<FcmToken> listActiveByMemberId(Long memberId) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByMemberIds(List<Long> memberIds) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByToken(String fcmToken) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByIds(List<Long> ids) {
            return tokens;
        }
    }

    private static class FakeSaveFcmPort implements SaveFcmPort {

        private final List<FcmToken> saved = new ArrayList<>();

        @Override
        public void save(FcmToken newToken) {
            saved.add(newToken);
        }
    }

    private static class FakeSendFcmMessagePort implements SendFcmMessagePort {

        private final FcmSendResult result;
        private FcmSendRequest lastRequest;

        private FakeSendFcmMessagePort(FcmSendResult result) {
            this.result = result;
        }

        @Override
        public FcmSendResult send(FcmSendRequest request) {
            this.lastRequest = request;
            return result;
        }
    }
}
