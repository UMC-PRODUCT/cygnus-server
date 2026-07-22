package com.umc.product.community.application.service.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.community.adapter.out.persistence.CommunityThreadMemberRepository;
import com.umc.product.community.adapter.out.persistence.CommunityThreadRepository;
import com.umc.product.community.application.port.in.realtime.RelayCommunityThreadRealtimeEventUseCase;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace.TransactionCall;

@DisplayName("Community thread member.left realtime transaction")
class CommunityThreadLifecycleRealtimeRelayTransactionIntegrationTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");
    private static final long TIMEOUT_SECONDS = 10L;

    @Autowired
    RelayCommunityThreadRealtimeEventUseCase relayUseCase;
    @Autowired
    CommunityThreadLifecycleRealtimeRelay lifecycleRelay;
    @Autowired
    CommunityThreadRepository threadRepository;
    @Autowired
    CommunityThreadMemberRepository memberRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockitoBean
    CommunityThreadRealtimeBroadcastPort broadcastPort;

    PostgreSqlTransactionRace race;
    ExecutorService relayExecutor;

    @BeforeEach
    void setUpConcurrency() {
        race = new PostgreSqlTransactionRace(transactionManager, jdbcTemplate);
        relayExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void closeConcurrency() {
        race.close();
        relayExecutor.shutdownNow();
    }

    @Test
    @DisplayName("package-private relay transaction은 broadcast가 끝날 때까지 concurrent rejoin을 막는다")
    void relayTransactionHoldsMembershipLockUntilBroadcastCompletes() throws Exception {
        Long threadId = persistLeftMembership();
        CountDownLatch broadcastStarted = new CountDownLatch(1);
        CountDownLatch releaseBroadcast = new CountDownLatch(1);
        AtomicBoolean transactionActiveDuringBroadcast = new AtomicBoolean();
        willAnswer(invocation -> {
            transactionActiveDuringBroadcast.set(
                TransactionSynchronizationManager.isActualTransactionActive()
            );
            broadcastStarted.countDown();
            race.await(releaseBroadcast, "broadcast release");
            return null;
        }).given(broadcastPort).broadcastToMember(eq(20L), any());

        Future<?> relay = relayExecutor.submit(() -> relayUseCase.relay(memberLeftEvent(threadId)));
        race.await(broadcastStarted, "broadcast start");
        TransactionCall<Void> rejoin = race.submit(() -> rejoin(threadId));
        try {
            race.awaitPostgreSqlLockWait(rejoin);
            assertThat(rejoin.future().isDone()).isFalse();
            assertThat(transactionActiveDuringBroadcast.get()).isTrue();
            assertThat(AopUtils.isCglibProxy(lifecycleRelay)).isTrue();
        } finally {
            releaseBroadcast.countDown();
        }

        relay.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        rejoin.future().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        CommunityThreadMember rejoined = memberRepository
            .findByThreadIdAndMemberId(threadId, 20L)
            .orElseThrow();
        assertThat(rejoined.isActive()).isTrue();
        assertThat(rejoined.getJoinedAt()).isEqualTo(NOW.plusSeconds(60));
        then(broadcastPort).should().broadcastToMember(eq(20L), any());
    }

    private Long persistLeftMembership() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> {
            CommunityThread thread = threadRepository.save(CommunityThread.create(
                501L,
                "동시성 스레드",
                null,
                CommunityThreadCategory.FREE,
                "chat",
                10L,
                NOW.minusSeconds(60)
            ));
            CommunityThreadMember member = CommunityThreadMember.createMember(
                thread.getId(),
                20L,
                NOW.minusSeconds(60)
            );
            member.leave(NOW);
            memberRepository.saveAndFlush(member);
            return thread.getId();
        });
    }

    private Void rejoin(Long threadId) {
        CommunityThreadMember member = memberRepository
            .findByThreadIdAndMemberId(threadId, 20L)
            .orElseThrow();
        member.rejoin(NOW.plusSeconds(60));
        memberRepository.saveAndFlush(member);
        return null;
    }

    private CommunityThreadMemberLeftEvent memberLeftEvent(Long threadId) {
        return CommunityThreadMemberLeftEvent.of(
            threadId,
            20L,
            List.of(20L),
            NOW.minusSeconds(60),
            NOW
        );
    }
}
