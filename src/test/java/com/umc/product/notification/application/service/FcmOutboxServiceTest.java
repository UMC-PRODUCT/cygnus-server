package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.notification.application.port.in.ProcessFcmOutboxUseCase;
import com.umc.product.notification.application.port.out.LoadFcmOutboxPort;
import com.umc.product.notification.domain.FcmOutbox;
import com.umc.product.notification.domain.FcmOutboxStatus;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.FcmOutboxFixture;
import com.umc.product.support.fixture.MemberFixture;

class FcmOutboxServiceTest extends UseCaseTestSupport {

    @Autowired
    private ProcessFcmOutboxUseCase processFcmOutboxUseCase;

    @Autowired
    private LoadFcmOutboxPort loadFcmOutboxPort;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private FcmOutboxFixture fcmOutboxFixture;

    @Test
    void SUBSCRIBE_이벤트는_토픽_기반_비활성화로_인해_즉시_FAILED_처리된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        FcmOutbox event = fcmOutboxFixture.구독_이벤트(memberId);

        // when
        processFcmOutboxUseCase.process();

        // then - 토픽 구독 로직이 비활성화되어 PENDING 이벤트 없이 FAILED 처리
        assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty();
        entityManager.clear();
        FcmOutbox persisted = entityManager.find(FcmOutbox.class, event.getId());
        assertThat(persisted.getStatus()).isEqualTo(FcmOutboxStatus.FAILED);
    }

    @Test
    void UNSUBSCRIBE_이벤트는_토픽_기반_비활성화로_인해_즉시_FAILED_처리된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        fcmOutboxFixture.구독해제_이벤트(memberId, "old-token");

        // when
        processFcmOutboxUseCase.process();

        // then
        assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty();
    }

    @Test
    void PENDING_이벤트가_없으면_아무것도_처리하지_않는다() {
        // when & then - 예외 없이 정상 종료
        processFcmOutboxUseCase.process();
        assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty();
    }

    @Test
    void 여러_PENDING_이벤트가_있으면_모두_FAILED_처리된다() {
        // given
        Long memberId = memberFixture.일반("테스터").getId();
        fcmOutboxFixture.구독_이벤트(memberId);
        fcmOutboxFixture.구독_이벤트(memberId);
        fcmOutboxFixture.구독해제_이벤트(memberId, "some-token");

        // when
        processFcmOutboxUseCase.process();

        // then
        assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty();
    }
}
