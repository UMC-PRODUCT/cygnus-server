package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadStompProbe.StompFrame;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Actor;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Scenario;

final class CommunityThreadLifecycleE2EScenario {

    private static final Duration RECEIPT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration NO_LEAK_WINDOW = Duration.ofSeconds(2);
    private static final String FIRST_CONTENT = "소유권 이전 뒤 attacker owner 메시지";
    private static final String SECOND_CONTENT = "leave와 kick 뒤 owner 전용 메시지";

    private final CommunityThreadTwoInstanceTopology topology;
    private final Scenario scenario;
    private final ObjectMapper objectMapper;
    private final CommunityThreadFrameAwaiter awaiter;

    CommunityThreadLifecycleE2EScenario(
        CommunityThreadTwoInstanceTopology topology,
        Scenario scenario,
        ObjectMapper objectMapper
    ) {
        this.topology = topology;
        this.scenario = scenario;
        this.objectMapper = objectMapper;
        this.awaiter = new CommunityThreadFrameAwaiter(objectMapper);
    }

    void run() throws Exception {
        try (
            CommunityThreadStompProbe owner = connect(topology.appA(), scenario.owner());
            CommunityThreadStompProbe member = connect(topology.appB(), scenario.member());
            CommunityThreadStompProbe attacker = connect(topology.appB(), scenario.attacker());
            CommunityThreadE2EHttpClient http = new CommunityThreadE2EHttpClient(
                topology.appB().port(),
                objectMapper
            )
        ) {
            BlockingQueue<StompFrame> ownerFrames = subscribeEvents(owner);
            BlockingQueue<StompFrame> memberFrames = subscribeEvents(member);
            BlockingQueue<StompFrame> attackerFrames = attacker.subscribe(
                CommunityThreadE2EProtocol.userEvents(),
                RECEIPT_TIMEOUT
            );
            awaitCrossServerUserRegistry(scenario.member().memberId());
            awaitCrossServerUserRegistry(scenario.attacker().memberId());

            // given/when: app B REST에서 제3의 멤버를 초대하고 소유권을 이전한다.
            inviteAndTransfer(http, ownerFrames, memberFrames, attackerFrames);

            // then: leave/kick terminal event 이후 기존 subscription에는 state가 누출되지 않는다.
            leaveFormerOwner(http, ownerFrames, attackerFrames);
            createAndVerifyFormerOwnerIsolation(
                attacker,
                attackerFrames,
                memberFrames,
                ownerFrames
            );
            kickMemberAndVerifyIsolation(http, attacker, attackerFrames, memberFrames, ownerFrames);
            verifySoftDeleteRetainsChatRows(http, attackerFrames);
        }
    }

    private void inviteAndTransfer(
        CommunityThreadE2EHttpClient http,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        BlockingQueue<StompFrame> attackerFrames
    ) throws Exception {
        JsonNode invitation = http.invite(
            scenario.threadId(),
            scenario.attacker().memberId(),
            scenario.owner().accessToken()
        );
        topology.relayOutbox();
        assertThat(invitation.at("/invitedMembers/0/memberId").asText())
            .isEqualTo(scenario.attacker().memberId().toString());
        JsonNode invited = awaiter.awaitType(attackerFrames, "thread.invited", EVENT_TIMEOUT);
        assertThat(invited.at("/payload/thread/threadId").asText())
            .isEqualTo(scenario.threadId().toString());
        assertNoRawRoomId(invited);
        awaiter.assertNoType(ownerFrames, "thread.invited", NO_LEAK_WINDOW);
        awaiter.assertNoType(memberFrames, "thread.invited", NO_LEAK_WINDOW);

        JsonNode transferred = http.transferOwnership(
            scenario.threadId(),
            scenario.attacker().memberId(),
            scenario.owner().accessToken()
        );
        assertThat(transferred.path("memberId").asText())
            .isEqualTo(scenario.attacker().memberId().toString());
        assertThat(transferred.path("role").asText()).isEqualTo("OWNER");
    }

    private void leaveFormerOwner(
        CommunityThreadE2EHttpClient http,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> attackerFrames
    ) throws Exception {
        JsonNode left = http.leave(scenario.threadId(), scenario.owner().accessToken());
        topology.relayOutbox();
        assertThat(left.path("memberId").asText()).isEqualTo(scenario.owner().memberId().toString());
        assertThat(left.path("state").asText()).isEqualTo("LEFT");

        JsonNode ownerTerminal = awaiter.awaitType(ownerFrames, "member.left", EVENT_TIMEOUT);
        JsonNode attackerTerminal = awaiter.awaitType(attackerFrames, "member.left", EVENT_TIMEOUT);
        assertThat(ownerTerminal.at("/payload/memberId").asLong())
            .isEqualTo(scenario.owner().memberId());
        assertThat(attackerTerminal.at("/payload/memberId").asLong())
            .isEqualTo(scenario.owner().memberId());
    }

    private void createAndVerifyFormerOwnerIsolation(
        CommunityThreadStompProbe attacker,
        BlockingQueue<StompFrame> attackerFrames,
        BlockingQueue<StompFrame> memberFrames,
        BlockingQueue<StompFrame> ownerFrames
    ) throws Exception {
        UUID commandId = sendMessage(attacker, FIRST_CONTENT);
        awaiter.awaitAck(attackerFrames, commandId, EVENT_TIMEOUT);
        topology.relayOutbox();
        JsonNode created = awaiter.awaitMessage(
            memberFrames,
            "message.created",
            FIRST_CONTENT,
            EVENT_TIMEOUT
        );
        assertThat(created.at("/payload/message/senderId").asLong())
            .isEqualTo(scenario.attacker().memberId());
        assertNoRawRoomId(created);
        awaiter.assertNoType(ownerFrames, "message.created", NO_LEAK_WINDOW);
    }

    private void kickMemberAndVerifyIsolation(
        CommunityThreadE2EHttpClient http,
        CommunityThreadStompProbe attacker,
        BlockingQueue<StompFrame> attackerFrames,
        BlockingQueue<StompFrame> memberFrames,
        BlockingQueue<StompFrame> ownerFrames
    ) throws Exception {
        JsonNode kicked = http.kick(
            scenario.threadId(),
            scenario.member().memberId(),
            scenario.attacker().accessToken()
        );
        topology.relayOutbox();
        assertThat(kicked.path("memberId").asText()).isEqualTo(scenario.member().memberId().toString());
        assertThat(kicked.path("state").asText()).isEqualTo("KICKED");
        JsonNode terminal = awaiter.awaitType(memberFrames, "member.kicked", EVENT_TIMEOUT);
        assertThat(terminal.at("/payload/memberId").asLong()).isEqualTo(scenario.member().memberId());
        memberFrames.clear();

        UUID commandId = sendMessage(attacker, SECOND_CONTENT);
        awaiter.awaitAck(attackerFrames, commandId, EVENT_TIMEOUT);
        topology.relayOutbox();
        awaiter.awaitMessage(attackerFrames, "message.created", SECOND_CONTENT, EVENT_TIMEOUT);
        awaiter.assertNoType(memberFrames, "message.created", NO_LEAK_WINDOW);
        awaiter.assertNoType(ownerFrames, "message.created", NO_LEAK_WINDOW);
    }

    private void verifySoftDeleteRetainsChatRows(
        CommunityThreadE2EHttpClient http,
        BlockingQueue<StompFrame> attackerFrames
    ) throws Exception {
        long beforeDelete = retainedMessageCount();
        assertThat(beforeDelete).isPositive();

        JsonNode deletedResponse = http.deleteThread(
            scenario.threadId(),
            scenario.attacker().accessToken()
        );
        topology.relayOutbox();
        assertThat(deletedResponse.path("threadId").asText()).isEqualTo(scenario.threadId().toString());
        assertThat(deletedResponse.path("deletedAt").isTextual()).isTrue();
        JsonNode deletedEvent = awaiter.awaitType(attackerFrames, "thread.deleted", EVENT_TIMEOUT);
        assertThat(deletedEvent.at("/payload/threadId").asText())
            .isEqualTo(scenario.threadId().toString());
        assertNoRawRoomId(deletedEvent);

        assertThat(retainedMessageCount()).isEqualTo(beforeDelete);
        CommunityThreadE2EHttpClient.HttpResult history = http.historyResult(
            scenario.threadId(),
            scenario.attacker().accessToken()
        );
        assertThat(history.status()).isEqualTo(404);
        assertThat(history.body().path("code").asText()).isEqualTo("COMMUNITY-0033");
    }

    private long retainedMessageCount() {
        JdbcTemplate jdbcTemplate = topology.appA().context().getBean(JdbcTemplate.class);
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM chat_message cm
                JOIN community_thread ct ON ct.chat_room_id = cm.room_id
                WHERE ct.id = ?
                """,
            Long.class,
            scenario.threadId()
        );
        return count == null ? 0L : count;
    }

    private UUID sendMessage(CommunityThreadStompProbe sender, String content) throws Exception {
        UUID commandId = UUID.randomUUID();
        sender.send(
            CommunityThreadE2EProtocol.messages(scenario.threadId()),
            commandId,
            CommunityThreadE2EProtocol.createMessage(UUID.randomUUID(), content, List.of(), null)
        );
        return commandId;
    }

    private BlockingQueue<StompFrame> subscribeEvents(CommunityThreadStompProbe probe)
        throws InterruptedException {
        return probe.subscribe(
            CommunityThreadE2EProtocol.userEvents(),
            RECEIPT_TIMEOUT
        );
    }

    private CommunityThreadStompProbe connect(
        CommunityThreadTwoInstanceTopology.AppInstance app,
        Actor actor
    ) throws Exception {
        return CommunityThreadStompProbe.connect(app.port(), actor.accessToken(), objectMapper);
    }

    private void awaitCrossServerUserRegistry(Long memberId) throws InterruptedException {
        SimpUserRegistry source = topology.appB().context().getBean(SimpUserRegistry.class);
        SimpUserRegistry target = topology.appA().context().getBean(SimpUserRegistry.class);
        long deadline = System.nanoTime() + EVENT_TIMEOUT.toNanos();
        do {
            if (hasConvergedSessions(source, target, memberId)) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        } while (System.nanoTime() < deadline);

        throw new AssertionError(
            "multi-server user registry가 제한 시간 안에 수렴하지 않았습니다: memberId=" + memberId
        );
    }

    private boolean hasConvergedSessions(
        SimpUserRegistry source,
        SimpUserRegistry target,
        Long memberId
    ) {
        SimpUser sourceUser = source.getUser(memberId.toString());
        SimpUser targetUser = target.getUser(memberId.toString());
        if (sourceUser == null || sourceUser.getSessions().isEmpty() || targetUser == null) {
            return false;
        }
        Set<String> sourceSessionIds = sourceUser.getSessions().stream()
            .map(session -> session.getId())
            .collect(java.util.stream.Collectors.toSet());
        Set<String> targetSessionIds = targetUser.getSessions().stream()
            .map(session -> session.getId())
            .collect(java.util.stream.Collectors.toSet());
        return targetSessionIds.containsAll(sourceSessionIds);
    }

    private void assertNoRawRoomId(JsonNode payload) {
        assertThat(payload.toString()).doesNotContain("chatRoomId").doesNotContain("\"roomId\"");
    }
}
