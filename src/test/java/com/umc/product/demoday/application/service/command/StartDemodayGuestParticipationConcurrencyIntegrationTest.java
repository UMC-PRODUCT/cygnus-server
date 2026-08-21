package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.HashDemodayParticipationRequestIdPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("게스트 참여 시작 동시 제출")
class StartDemodayGuestParticipationConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final String ADMISSION_CODE = "GUEST-CONCURRENT";
    private static final String FIRST_REQUEST_ID = "0f43f02a-6ecf-4bb3-82ce-625029bd3e09";
    private static final String SECOND_REQUEST_ID = "b716e41f-48c2-47bc-8f43-1b44bb890f48";

    @Autowired private StartDemodayGuestParticipationUseCase startDemodayGuestParticipationUseCase;
    @Autowired private SaveDemodayPollPort saveDemodayPollPort;
    @Autowired private SaveDemodayEntryCodePort saveDemodayEntryCodePort;
    @Autowired private LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    @Autowired private HashDemodayEntryCodePort hashDemodayEntryCodePort;
    @Autowired private HashDemodayParticipationRequestIdPort hashDemodayParticipationRequestIdPort;
    @Autowired private Clock clock;

    @Test
    @DisplayName("서로 다른 요청 식별자가 같은 코드를 동시에 제출하면 하나만 코드에 귀속된다")
    void acceptOnlyOneConcurrentRequestId() throws Exception {
        // given
        Instant now = clock.instant();
        DemodayPoll poll = saveDemodayPollPort.save(
            DemodayPoll.create(1L, "게스트 동시 제출 Poll", now.minusSeconds(60), now.plusSeconds(3600)));
        String codeHash = hashDemodayEntryCodePort.hash(ADMISSION_CODE);
        saveDemodayEntryCodePort.save(DemodayEntryCode.create(poll.getId(), codeHash));

        // when
        List<Optional<DemodayErrorCode>> results = race(poll.getId());

        // then
        assertThat(results).containsExactlyInAnyOrder(
            Optional.empty(),
            Optional.of(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED)
        );

        DemodayEntryCode entryCode = loadDemodayEntryCodePort.findByCodeHash(codeHash).orElseThrow();
        assertThat(entryCode.getRedemptionRequestIdHash()).isIn(
            hashDemodayParticipationRequestIdPort.hash(FIRST_REQUEST_ID),
            hashDemodayParticipationRequestIdPort.hash(SECOND_REQUEST_ID)
        );
    }

    private List<Optional<DemodayErrorCode>> race(Long pollId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<DemodayErrorCode>> first = executor.submit(
                () -> startAfterSignal(pollId, FIRST_REQUEST_ID, ready, start));
            Future<Optional<DemodayErrorCode>> second = executor.submit(
                () -> startAfterSignal(pollId, SECOND_REQUEST_ID, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Optional<DemodayErrorCode> startAfterSignal(
        Long pollId,
        String requestId,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("demoday guest participation concurrency start timed out");
        }

        try {
            startDemodayGuestParticipationUseCase.start(
                new StartDemodayGuestParticipationCommand(pollId, ADMISSION_CODE, requestId, null));
            return Optional.empty();
        } catch (DemodayDomainException exception) {
            return Optional.of((DemodayErrorCode) exception.getBaseCode());
        }
    }
}
