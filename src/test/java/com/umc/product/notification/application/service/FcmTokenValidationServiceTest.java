package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.in.dto.FcmTokenValidationInfo;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.ValidateFcmTokenPort;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;
import com.umc.product.notification.domain.FcmToken;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("FCM 토큰 유효성 검증 서비스")
class FcmTokenValidationServiceTest {

    @Test
    @DisplayName("검증 대상 토큰을 FCM dry-run으로 확인하고 유효하지 않은 토큰은 비활성화한다")
    void due_tokens_검증() {
        // given
        FcmToken valid = staleToken(1L, 10L, "valid-token");
        FcmToken invalid = staleToken(2L, 20L, "invalid-token");
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(valid, invalid));
        FakeSaveFcmPort saveFcmPort = new FakeSaveFcmPort();
        FakeValidateFcmTokenPort validateFcmTokenPort = new FakeValidateFcmTokenPort(
            FcmTokenValidationResult.of(1, 1, List.of(1L), List.of(2L))
        );
        FcmTokenValidationService service = new FcmTokenValidationService(
            new FcmProperties(true, true),
            loadFcmPort,
            saveFcmPort,
            validateFcmTokenPort,
            new OperationalMetrics(new SimpleMeterRegistry()),
            500,
            Duration.ofDays(30)
        );

        // when
        FcmTokenValidationInfo info = service.validateDueTokens();

        // then
        assertThat(validateFcmTokenPort.lastRequest.targets()).hasSize(2);
        assertThat(info.requestedCount()).isEqualTo(2);
        assertThat(info.invalidatedCount()).isEqualTo(1);
        assertThat(invalid.isActive()).isFalse();
        assertThat(invalid.getDeactivatedAt()).isNotNull();
        assertThat(valid.isActive()).isTrue();
        assertThat(valid.getLastValidatedAt()).isNotNull();
        assertThat(saveFcmPort.saved).containsExactlyInAnyOrder(valid, invalid);
        assertThat(saveFcmPort.saveAllCallCount).isEqualTo(1);
        assertThat(loadFcmPort.lastLimit).isEqualTo(500);
        assertThat(loadFcmPort.lastValidatedBefore).isBeforeOrEqualTo(Instant.now().minus(Duration.ofDays(29)));
    }

    @Test
    @DisplayName("토큰별 transient 실패는 검증 성공으로 기록하지 않는다")
    void transient_token_failure_검증_시각_미기록() {
        // given
        FcmToken valid = staleToken(1L, 10L, "valid-token");
        FcmToken transientFailure = staleToken(2L, 20L, "transient-token");
        Instant transientFailureLastValidatedAt = transientFailure.getLastValidatedAt();
        FakeSaveFcmPort saveFcmPort = new FakeSaveFcmPort();
        FcmTokenValidationService service = new FcmTokenValidationService(
            new FcmProperties(true, true),
            new FakeLoadFcmPort(List.of(valid, transientFailure)),
            saveFcmPort,
            new FakeValidateFcmTokenPort(FcmTokenValidationResult.of(1, 1, List.of(1L), List.of())),
            new OperationalMetrics(new SimpleMeterRegistry()),
            500,
            Duration.ofDays(30)
        );

        // when
        service.validateDueTokens();

        // then
        assertThat(valid.getLastValidatedAt()).isNotNull();
        assertThat(transientFailure.isActive()).isTrue();
        assertThat(transientFailure.getLastValidatedAt()).isEqualTo(transientFailureLastValidatedAt);
        assertThat(saveFcmPort.saved).containsExactly(valid);
        assertThat(saveFcmPort.saveAllCallCount).isEqualTo(1);
    }

    @Test
    @DisplayName("FCM이 비활성화되어 있으면 토큰을 조회하지 않는다")
    void fcm_disabled_스킵() {
        // given
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(staleToken(1L, 10L, "token")));
        FcmTokenValidationService service = new FcmTokenValidationService(
            new FcmProperties(false, true),
            loadFcmPort,
            new FakeSaveFcmPort(),
            new FakeValidateFcmTokenPort(FcmTokenValidationResult.of(0, 0, List.of(), List.of())),
            new OperationalMetrics(new SimpleMeterRegistry()),
            500,
            Duration.ofDays(30)
        );

        // when
        FcmTokenValidationInfo info = service.validateDueTokens();

        // then
        assertThat(info.requestedCount()).isZero();
        assertThat(loadFcmPort.called).isFalse();
    }

    private FcmToken staleToken(Long id, Long memberId, String value) {
        FcmToken token = FcmToken.create(memberId, value);
        ReflectionTestUtils.setField(token, "id", id);
        ReflectionTestUtils.setField(token, "lastValidatedAt", Instant.now().minus(Duration.ofDays(31)));
        return token;
    }

    private static class FakeLoadFcmPort implements LoadFcmPort {

        private final List<FcmToken> tokens;
        private boolean called;
        private Instant lastValidatedBefore;
        private int lastLimit;

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
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveForValidation(Instant validatedBefore, int limit) {
            this.called = true;
            this.lastValidatedBefore = validatedBefore;
            this.lastLimit = limit;
            return tokens;
        }
    }

    private static class FakeSaveFcmPort implements SaveFcmPort {

        private final List<FcmToken> saved = new ArrayList<>();
        private int saveAllCallCount;

        @Override
        public void save(FcmToken newToken) {
            saved.add(newToken);
        }

        @Override
        public void saveAll(List<FcmToken> fcmTokens) {
            saveAllCallCount++;
            saved.addAll(fcmTokens);
        }
    }

    private static class FakeValidateFcmTokenPort implements ValidateFcmTokenPort {

        private final FcmTokenValidationResult result;
        private FcmTokenValidationRequest lastRequest;

        private FakeValidateFcmTokenPort(FcmTokenValidationResult result) {
            this.result = result;
        }

        @Override
        public FcmTokenValidationResult validate(FcmTokenValidationRequest request) {
            this.lastRequest = request;
            return result;
        }
    }
}
