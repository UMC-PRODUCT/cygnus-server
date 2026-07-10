package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.domain.FcmToken;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.FcmTokenFixture;
import com.umc.product.support.fixture.MemberFixture;

class FcmServiceTest extends UseCaseTestSupport {

    @Autowired
    private ManageFcmUseCase manageFcmUseCase;

    @Autowired
    private LoadFcmPort loadFcmPort;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private FcmTokenFixture fcmTokenFixture;

    @Test
    void 신규_토큰_등록_시_FCM_토큰이_활성_상태로_저장된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        RegisterFcmTokenCommand command = RegisterFcmTokenCommand.of(
            memberId, "new-token", "IOS", "device-1", "1.0.0"
        );

        // when
        manageFcmUseCase.registerFcmToken(command);

        // then
        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(memberId);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getFcmToken()).isEqualTo("new-token");
        assertThat(tokens.get(0).isActive()).isTrue();
        assertThat(tokens.get(0).getPlatform()).isEqualTo("IOS");
        assertThat(tokens.get(0).getDeviceId()).isEqualTo("device-1");
        assertThat(tokens.get(0).getAppVersion()).isEqualTo("1.0.0");
    }

    @Test
    void 동일_토큰_재등록_시_INSERT_없이_활성화만_된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        FcmToken existing = fcmTokenFixture.FCM_토큰(memberId, "existing-token");
        existing.deactivate();

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(memberId, "existing-token", "ANDROID", null, null)
        );

        // then - 새 레코드 추가 없이 기존 토큰이 활성화됨
        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(memberId);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getFcmToken()).isEqualTo("existing-token");
        assertThat(tokens.get(0).isActive()).isTrue();
        assertThat(tokens.get(0).getPlatform()).isEqualTo("ANDROID");
    }

    @Test
    void 새_기기_토큰_등록_시_기존_토큰과_함께_저장된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        fcmTokenFixture.FCM_토큰(memberId, "old-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(memberId, "new-device-token", null, null, null)
        );

        // then - 기존 토큰 유지 + 새 토큰 추가
        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(memberId);
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(FcmToken::getFcmToken)
            .containsExactlyInAnyOrder("old-token", "new-device-token");
    }

    @Test
    void 다른_회원의_활성_토큰을_등록하면_이전_회원의_토큰은_비활성화된다() {
        // given
        Long previousMemberId = memberFixture.일반("기존회원").getId();
        Long newMemberId = memberFixture.일반("신규회원").getId();
        fcmTokenFixture.FCM_토큰(previousMemberId, "shared-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(newMemberId, "shared-token", null, null, null)
        );

        // then
        assertThat(loadFcmPort.listActiveByMemberId(previousMemberId)).isEmpty();
        assertThat(loadFcmPort.listActiveByMemberId(newMemberId))
            .extracting(FcmToken::getFcmToken)
            .containsExactly("shared-token");
    }

    @Test
    void 토큰_해제_시_해당_회원의_토큰만_비활성화된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        fcmTokenFixture.FCM_토큰(memberId, "delete-token");
        fcmTokenFixture.FCM_토큰(memberId, "keep-token");

        // when
        manageFcmUseCase.unregisterFcmToken(UnregisterFcmTokenCommand.of(memberId, "delete-token"));

        // then
        assertThat(loadFcmPort.listActiveByMemberId(memberId))
            .extracting(FcmToken::getFcmToken)
            .containsExactly("keep-token");
    }
}
