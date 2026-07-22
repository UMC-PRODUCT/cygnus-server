package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadStompProbe.StompFrame;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Actor;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Scenario;

final class CommunityThreadCommandE2EScenario {

    private static final Duration RECEIPT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration NO_DUPLICATE_WINDOW = Duration.ofSeconds(2);
    private static final String BASE_CONTENT = "두 인스턴스 command 기준 메시지";
    private static final String REPLY_CONTENT = "답장과 멘션을 함께 보냅니다";

    private final CommunityThreadTwoInstanceTopology topology;
    private final Scenario scenario;
    private final ObjectMapper objectMapper;
    private final CommunityThreadFrameAwaiter awaiter;
    private final CommunityThreadOutboxProbe outbox;

    CommunityThreadCommandE2EScenario(
        CommunityThreadTwoInstanceTopology topology,
        Scenario scenario,
        ObjectMapper objectMapper
    ) {
        this.topology = topology;
        this.scenario = scenario;
        this.objectMapper = objectMapper;
        this.awaiter = new CommunityThreadFrameAwaiter(objectMapper);
        this.outbox = new CommunityThreadOutboxProbe(topology.appA().context());
    }

    void run() throws Exception {
        try (
            CommunityThreadStompProbe owner = connect(topology.appA(), scenario.owner());
            CommunityThreadStompProbe ownerMirror = connect(topology.appB(), scenario.owner());
            CommunityThreadStompProbe member = connect(topology.appB(), scenario.member())
        ) {
            Channels channels = new Channels(
                owner.subscribe(CommunityThreadE2EProtocol.userEvents(), RECEIPT_TIMEOUT),
                ownerMirror.subscribe(CommunityThreadE2EProtocol.userEvents(), RECEIPT_TIMEOUT),
                member.subscribe(CommunityThreadE2EProtocol.userEvents(), RECEIPT_TIMEOUT),
                owner.subscribe("/user/queue/errors", RECEIPT_TIMEOUT)
            );
            topology.awaitUserRegistry(scenario.owner().memberId(), 2);
            topology.awaitUserRegistry(scenario.member().memberId(), 1);

            Long baseMessageId = createAndVerifyIdempotency(owner, channels);
            Long replyMessageId = createReplyWithMention(member, channels, baseMessageId);
            new CommunityThreadMutationE2EVerifier(topology, scenario, awaiter).verify(
                owner,
                member,
                channels.ownerFrames(),
                channels.memberFrames(),
                channels.ownerErrors(),
                replyMessageId
            );
        }
    }

    private Long createAndVerifyIdempotency(
        CommunityThreadStompProbe owner,
        Channels channels
    ) throws Exception {
        UUID clientMessageId = UUID.randomUUID();
        Map<String, Object> body = CommunityThreadE2EProtocol.createMessage(
            clientMessageId,
            BASE_CONTENT,
            List.of(),
            null
        );

        // given/when: app A owner가 최초 create command를 전송한다.
        UUID createCommandId = UUID.randomUUID();
        owner.send(CommunityThreadE2EProtocol.messages(scenario.threadId()), createCommandId, body);
        JsonNode createAck = assertAck(
            channels.ownerFrames(),
            createCommandId,
            "MESSAGE_CREATE",
            false
        );
        assertAck(channels.ownerMirrorFrames(), createCommandId, "MESSAGE_CREATE", false);
        awaitCommittedMessage(createAck);
        topology.relayOutbox();

        // then: app B member가 state event를 받고 caller는 correlated ACK를 받는다.
        JsonNode created = awaiter.awaitMessage(
            channels.memberFrames(),
            "message.created",
            BASE_CONTENT,
            EVENT_TIMEOUT
        );
        assertEnvelope(created);
        JsonNode message = created.at("/payload/message");
        Long messageId = message.path("messageId").asLong();
        assertThat(messageId).isPositive();
        assertThat(message.path("clientMessageId").asText()).isEqualTo(clientMessageId.toString());
        JsonNode mirrored = awaiter.awaitMessage(
            channels.ownerMirrorFrames(),
            "message.created",
            BASE_CONTENT,
            EVENT_TIMEOUT
        );
        assertThat(mirrored.at("/payload/message/messageId").asLong()).isEqualTo(messageId);

        UUID retryCommandId = UUID.randomUUID();
        owner.send(CommunityThreadE2EProtocol.messages(scenario.threadId()), retryCommandId, body);
        JsonNode retryAck = assertAck(
            channels.ownerFrames(),
            retryCommandId,
            "MESSAGE_CREATE",
            true
        );
        assertThat(retryAck.at("/payload/messageId").asLong()).isEqualTo(messageId);
        assertThat(retryAck.at("/payload/clientMessageId").asText())
            .isEqualTo(clientMessageId.toString());
        awaiter.assertNoType(channels.memberFrames(), "message.created", NO_DUPLICATE_WINDOW);

        UUID conflictCommandId = UUID.randomUUID();
        owner.send(
            CommunityThreadE2EProtocol.messages(scenario.threadId()),
            conflictCommandId,
            CommunityThreadE2EProtocol.createMessage(
                clientMessageId,
                "같은 clientMessageId의 다른 payload",
                List.of(),
                null
            )
        );
        JsonNode conflict = awaiter.awaitError(
            channels.ownerErrors(),
            conflictCommandId,
            EVENT_TIMEOUT
        );
        assertThat(conflict.path("clientMessageId").asText()).isEqualTo(clientMessageId.toString());
        assertThat(conflict.path("status").asInt()).isEqualTo(409);
        assertThat(conflict.path("code").asText()).isEqualTo("CHAT-0014");
        assertThat(conflict.path("retryable").asBoolean()).isFalse();
        return messageId;
    }

    private Long createReplyWithMention(
        CommunityThreadStompProbe member,
        Channels channels,
        Long baseMessageId
    ) throws Exception {
        UUID commandId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        member.send(
            CommunityThreadE2EProtocol.messages(scenario.threadId()),
            commandId,
            CommunityThreadE2EProtocol.createMessage(
                clientMessageId,
                REPLY_CONTENT,
                List.of(scenario.owner().memberId()),
                baseMessageId
            )
        );
        JsonNode createAck = assertAck(
            channels.memberFrames(),
            commandId,
            "MESSAGE_CREATE",
            false
        );
        awaitCommittedMessage(createAck);
        topology.relayOutbox();

        JsonNode created = awaiter.awaitMessage(
            channels.ownerFrames(),
            "message.created",
            REPLY_CONTENT,
            EVENT_TIMEOUT
        );
        assertEnvelope(created);
        JsonNode message = created.at("/payload/message");
        Long replyMessageId = message.path("messageId").asLong();
        assertThat(message.at("/replyTo/messageId").asLong()).isEqualTo(baseMessageId);
        assertThat(message.path("mentions").findValuesAsText("memberId"))
            .containsExactly(scenario.owner().memberId().toString());
        assertThat(created.at("/payload/clientMessageId").asText())
            .isEqualTo(clientMessageId.toString());
        return replyMessageId;
    }

    private JsonNode assertAck(
        BlockingQueue<StompFrame> frames,
        UUID commandId,
        String command,
        boolean deduplicated
    ) throws Exception {
        JsonNode ack = awaiter.awaitAck(frames, commandId, EVENT_TIMEOUT);
        assertThat(ack.at("/payload/command").asText()).isEqualTo(command);
        assertThat(ack.at("/payload/deduplicated").asBoolean()).isEqualTo(deduplicated);
        return ack;
    }

    private void awaitCommittedMessage(JsonNode acknowledgement) {
        long messageId = acknowledgement.at("/payload/messageId").asLong();
        assertThat(messageId).isPositive();
        outbox.awaitMessageCreated(messageId, EVENT_TIMEOUT);
    }

    private void assertEnvelope(JsonNode event) {
        assertThat(event.path("eventId").asText()).isNotBlank();
        assertThat(event.path("threadId").asText()).isEqualTo(scenario.threadId().toString());
        assertNoRawRoomId(event);
    }

    private void assertNoRawRoomId(JsonNode payload) {
        assertThat(payload.toString()).doesNotContain("chatRoomId").doesNotContain("\"roomId\"");
    }

    private CommunityThreadStompProbe connect(
        CommunityThreadTwoInstanceTopology.AppInstance app,
        Actor actor
    ) throws Exception {
        return CommunityThreadStompProbe.connect(app.port(), actor.accessToken(), objectMapper);
    }

    private record Channels(
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> ownerMirrorFrames,
        BlockingQueue<StompFrame> memberFrames,
        BlockingQueue<StompFrame> ownerErrors
    ) {
    }
}
