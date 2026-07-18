package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadOutboxProbe.OutboxRow;
import com.umc.product.community.adapter.in.websocket.CommunityThreadStompProbe.StompFrame;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Actor;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Scenario;

final class CommunityThreadBrokerFailureE2EScenario {

    private static final Duration RECEIPT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);

    private final CommunityThreadTwoInstanceTopology topology;
    private final Scenario scenario;
    private final ObjectMapper objectMapper;
    private final CommunityThreadFrameAwaiter awaiter;
    private final CommunityThreadOutboxProbe outbox;

    CommunityThreadBrokerFailureE2EScenario(
        CommunityThreadTwoInstanceTopology topology,
        Scenario scenario,
        ObjectMapper objectMapper
    ) {
        this.topology = topology;
        this.scenario = scenario;
        this.objectMapper = objectMapper;
        awaiter = new CommunityThreadFrameAwaiter(objectMapper);
        outbox = new CommunityThreadOutboxProbe(topology.appA().context());
    }

    void verifyRecovery() throws Exception {
        assertSpoofSubscriptionIsTerminal();
        OutboxRow failedAttempt;
        double retryBefore = outbox.retryCount();

        // given: owner/app A와 member·attacker/app B session이 연결된 뒤 task-owned relay만 중단한다.
        try (
            CommunityThreadStompProbe owner = connect(topology.appA(), scenario.owner());
            CommunityThreadStompProbe member = connect(topology.appB(), scenario.member());
            CommunityThreadStompProbe attacker = connect(topology.appB(), scenario.attacker());
            CommunityThreadE2EHttpClient http = http(topology.appB())
        ) {
            owner.subscribe(threadTopic(scenario.owner()), RECEIPT_TIMEOUT);
            member.subscribe(threadTopic(scenario.member()), RECEIPT_TIMEOUT);
            attacker.subscribe(
                CommunityThreadE2EProtocol.personalTopic(scenario.attacker().memberId()),
                RECEIPT_TIMEOUT
            );
            topology.pauseRelay();

            // when: app B REST invite는 DOWN 상태에서도 commit되고 app A outbox publish만 실패한다.
            JsonNode invitation = inviteAttacker(http);
            Long invitedMemberId = invitation.at("/invitedMembers/0/memberId").asLong();
            assertThat(invitedMemberId).isEqualTo(scenario.attacker().memberId());
            OutboxRow committed = awaitInvitationOutbox(invitedMemberId);
            assertThat(committed.status()).isEqualTo("PENDING");
            assertThat(committed.attempts()).isZero();
            topology.relayOutbox();

            failedAttempt = outbox.get(committed.eventId());
            assertThat(failedAttempt.eventId()).isEqualTo(committed.eventId());
            assertThat(failedAttempt.status()).isEqualTo("PENDING");
            assertThat(failedAttempt.attempts()).isEqualTo(1);
            assertThat(failedAttempt.lastError()).isNotBlank();
            assertThat(outbox.retryCount()).isEqualTo(retryBefore + 1.0);

            JsonNode history = http.history(
                scenario.threadId(),
                scenario.attacker().accessToken()
            );
            assertThat(history.path("messages").isArray()).isTrue();
            assertNoRawRoomId(history);
        } finally {
            resumeIfPaused();
        }

        // then: reconnect 후 같은 eventId가 personal topic에 전달되고 outbox가 PUBLISHED 된다.
        verifyReconnectPublishesStableEvent(failedAttempt);
    }

    void verifyExhaustion() throws Exception {
        double retryBefore = outbox.retryCount();
        double failedBefore = outbox.failedCount();
        OutboxRow exhausted;

        try (CommunityThreadE2EHttpClient http = http(topology.appB())) {
            topology.pauseRelay();
            JsonNode invitation = inviteAttacker(http);
            Long invitedMemberId = invitation.at("/invitedMembers/0/memberId").asLong();
            assertThat(invitedMemberId).isEqualTo(scenario.attacker().memberId());
            OutboxRow committed = awaitInvitationOutbox(invitedMemberId);

            topology.relayOutbox();
            OutboxRow firstFailure = outbox.get(committed.eventId());
            assertThat(firstFailure.status()).isEqualTo("PENDING");
            assertThat(firstFailure.attempts()).isEqualTo(1);
            assertThat(outbox.retryCount()).isEqualTo(retryBefore + 1.0);

            outbox.makePublishable(committed.eventId());
            topology.relayOutbox();
            exhausted = outbox.get(committed.eventId());
            assertThat(exhausted.status()).isEqualTo("FAILED");
            assertThat(exhausted.attempts()).isEqualTo(2);
            assertThat(exhausted.lastError()).isNotBlank();
            assertThat(outbox.failedCount()).isEqualTo(failedBefore + 1.0);
        } finally {
            resumeIfPaused();
        }

        topology.relayOutbox();
        assertThat(outbox.get(exhausted.eventId()).status()).isEqualTo("FAILED");
    }

    private void verifyReconnectPublishesStableEvent(OutboxRow failedAttempt) throws Exception {
        try (
            CommunityThreadStompProbe owner = connect(topology.appA(), scenario.owner());
            CommunityThreadStompProbe attacker = connect(topology.appB(), scenario.attacker())
        ) {
            owner.subscribe(threadTopic(scenario.owner()), RECEIPT_TIMEOUT);
            BlockingQueue<StompFrame> personalFrames = attacker.subscribe(
                CommunityThreadE2EProtocol.personalTopic(scenario.attacker().memberId()),
                RECEIPT_TIMEOUT
            );
            outbox.makePublishable(failedAttempt.eventId());
            topology.relayOutbox();

            JsonNode event = awaiter.awaitType(personalFrames, "thread.invited", EVENT_TIMEOUT);
            assertThat(event.path("eventId").asText()).isEqualTo(failedAttempt.eventId().toString());
            assertThat(event.at("/payload/thread/threadId").asText())
                .isEqualTo(scenario.threadId().toString());
            assertNoRawRoomId(event);
            OutboxRow published = outbox.get(failedAttempt.eventId());
            assertThat(published.status()).isEqualTo("PUBLISHED");
            assertThat(published.attempts()).isEqualTo(1);
        }
    }

    private JsonNode inviteAttacker(CommunityThreadE2EHttpClient http) throws Exception {
        return http.invite(
            scenario.threadId(),
            scenario.attacker().memberId(),
            scenario.owner().accessToken()
        );
    }

    private OutboxRow awaitInvitationOutbox(Long invitedMemberId) {
        return outbox.awaitInvitation(
            scenario.threadId(),
            invitedMemberId,
            EVENT_TIMEOUT
        );
    }

    private void assertSpoofSubscriptionIsTerminal() throws Exception {
        try (CommunityThreadStompProbe attacker = connect(topology.appB(), scenario.attacker())) {
            attacker.subscribeExpectingTerminalError(threadTopic(scenario.member()));
            JsonNode error = objectMapper.readTree(
                attacker.awaitTerminalError(EVENT_TIMEOUT).payload()
            );
            assertThat(error.path("code").asText()).isEqualTo("SECURITY-0004");
        }
    }

    private CommunityThreadStompProbe connect(
        CommunityThreadTwoInstanceTopology.AppInstance app,
        Actor actor
    ) throws Exception {
        return CommunityThreadStompProbe.connect(app.port(), actor.accessToken(), objectMapper);
    }

    private CommunityThreadE2EHttpClient http(CommunityThreadTwoInstanceTopology.AppInstance app) {
        return new CommunityThreadE2EHttpClient(app.port(), objectMapper);
    }

    private String threadTopic(Actor actor) {
        return CommunityThreadE2EProtocol.threadTopic(scenario.threadId(), actor.memberId());
    }

    private void resumeIfPaused() throws InterruptedException {
        if (topology.relayPaused()) {
            topology.resumeRelay();
        }
    }

    private void assertNoRawRoomId(JsonNode payload) {
        assertThat(payload.toString()).doesNotContain("chatRoomId").doesNotContain("\"roomId\"");
    }
}
