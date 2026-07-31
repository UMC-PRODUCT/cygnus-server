package com.umc.product.community.adapter.in.websocket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Participants;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Scenario;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Community thread 두 인스턴스 외부 STOMP relay E2E")
class CommunityThreadTwoInstanceRelayE2ETest {

    private CommunityThreadTwoInstanceTopology topology;
    private ObjectMapper objectMapper;
    private Participants participants;
    private Scenario scenario;
    private int threadSequence;

    @BeforeAll
    void startSharedTopology() {
        topology = CommunityThreadTwoInstanceTopology.start();
        try {
            objectMapper = topology.appA().context().getBean(ObjectMapper.class);
            participants = CommunityThreadTwoInstanceFixture.seedParticipants(
                topology.appA().context()
            );
        } catch (RuntimeException exception) {
            try {
                topology.close();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            topology = null;
            throw exception;
        }
    }

    @BeforeEach
    void createIsolatedThread() {
        scenario = CommunityThreadTwoInstanceFixture.createThread(
            topology.appA().context(),
            participants,
            ++threadSequence
        );
        topology.relayOutbox();
    }

    @AfterAll
    void stopOwnedTopology() {
        if (topology != null) {
            topology.close();
        }
    }

    @Test
    @DisplayName("실제 STOMP command 전체 surface와 correlation, idempotency, rate limit을 검증한다")
    void exercisesFullCommandSurface() throws Exception {
        new CommunityThreadCommandE2EScenario(topology, scenario, objectMapper).run();
    }

    @Test
    @DisplayName("relay DOWN commit을 REST로 복구하고 stable eventId로 재발행한다")
    void recoversCommittedOutboxAfterBrokerReconnect() throws Exception {
        new CommunityThreadBrokerFailureE2EScenario(topology, scenario, objectMapper)
            .verifyRecovery();
    }

    @Test
    @DisplayName("실제 relay 실패가 max attempts를 소진하면 outbox와 metric이 FAILED가 된다")
    void marksOutboxFailedAfterRelayAttemptsExhausted() throws Exception {
        new CommunityThreadBrokerFailureE2EScenario(topology, scenario, objectMapper)
            .verifyExhaustion();
    }

    @Test
    @Disabled("CI 에서 STOMP frame timeout 으로 flaky. community 담당자와 협의 후 별도 이슈로 정리 예정.")
    @DisplayName("공통 user queue에서 초대·leave·kick 대상과 stale 격리를 검증한다")
    void exercisesMembershipAndSoftDeleteLifecycle() throws Exception {
        new CommunityThreadLifecycleE2EScenario(topology, scenario, objectMapper).run();
    }
}
