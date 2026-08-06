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
            memberId, "installation-1", "new-token", "IOS", "1.0.0"
        );

        // when
        manageFcmUseCase.registerFcmToken(command);

        // then
        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(memberId);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getFcmToken()).isEqualTo("new-token");
        assertThat(tokens.get(0).isActive()).isTrue();
        assertThat(tokens.get(0).getPlatform()).isEqualTo("IOS");
        assertThat(tokens.get(0).getInstallationId()).isEqualTo("installation-1");
        assertThat(tokens.get(0).getAppVersion()).isEqualTo("1.0.0");
    }

    @Test
    void 동일_토큰_재등록_시_INSERT_없이_활성화만_된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        FcmToken existing = fcmTokenFixture.FCM_토큰(memberId, "installation-1", "existing-token");
        existing.deactivate();

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(memberId, "installation-1", "existing-token", "ANDROID", null)
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
        fcmTokenFixture.FCM_토큰(memberId, "installation-1", "old-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(memberId, "installation-2", "new-device-token", null, null)
        );

        // then - 기존 토큰 유지 + 새 토큰 추가
        List<FcmToken> tokens = loadFcmPort.listActiveByMemberId(memberId);
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(FcmToken::getFcmToken)
            .containsExactlyInAnyOrder("old-token", "new-device-token");
    }

    @Test
    void 동일_installation에서_토큰이_변경되면_기존_row를_갱신한다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        fcmTokenFixture.FCM_토큰(memberId, "installation-1", "old-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(memberId, "installation-1", "new-token", "IOS", "2.0.0")
        );

        // then
        assertThat(loadFcmPort.listActiveByMemberId(memberId))
            .singleElement()
            .satisfies(token -> {
                assertThat(token.getInstallationId()).isEqualTo("installation-1");
                assertThat(token.getFcmToken()).isEqualTo("new-token");
            });
    }

    @Test
    void 동일_installation에_다른_회원이_로그인하면_소유권과_토큰을_교체한다() {
        // given
        Long previousMemberId = memberFixture.일반("기존회원").getId();
        Long newMemberId = memberFixture.일반("신규회원").getId();
        fcmTokenFixture.FCM_토큰(previousMemberId, "shared-installation", "previous-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(newMemberId, "shared-installation", "new-token", null, null)
        );

        // then
        assertThat(loadFcmPort.listActiveByMemberId(previousMemberId)).isEmpty();
        assertThat(loadFcmPort.listActiveByMemberId(newMemberId))
            .extracting(FcmToken::getFcmToken)
            .containsExactly("new-token");
    }

    @Test
    void 다른_installation에서_같은_토큰을_등록하면_이전_installation을_비활성화한다() {
        // given
        Long previousMemberId = memberFixture.일반("기존회원").getId();
        Long newMemberId = memberFixture.일반("신규회원").getId();
        fcmTokenFixture.FCM_토큰(previousMemberId, "previous-installation", "shared-token");

        // when
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(newMemberId, "new-installation", "shared-token", null, null)
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
        fcmTokenFixture.FCM_토큰(memberId, "delete-installation", "delete-token");
        fcmTokenFixture.FCM_토큰(memberId, "keep-installation", "keep-token");

        // when
        manageFcmUseCase.unregisterFcmToken(UnregisterFcmTokenCommand.of(memberId, "delete-installation"));

        // then
        assertThat(loadFcmPort.listActiveByMemberId(memberId))
            .extracting(FcmToken::getFcmToken)
            .containsExactly("keep-token");
    }

    @Test
    void 이전_회원의_늦은_로그아웃은_새_회원의_installation을_비활성화하지_않는다() {
        // given
        Long previousMemberId = memberFixture.일반("기존회원").getId();
        Long newMemberId = memberFixture.일반("신규회원").getId();
        fcmTokenFixture.FCM_토큰(previousMemberId, "shared-installation", "shared-token");
        manageFcmUseCase.registerFcmToken(
            RegisterFcmTokenCommand.of(newMemberId, "shared-installation", "shared-token", null, null)
        );

        // when
        manageFcmUseCase.unregisterFcmToken(
            UnregisterFcmTokenCommand.of(previousMemberId, "shared-installation")
        );

        // then
        assertThat(loadFcmPort.listActiveByMemberId(newMemberId))
            .extracting(FcmToken::getFcmToken)
            .containsExactly("shared-token");
    }
}
