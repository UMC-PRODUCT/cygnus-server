package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.community.adapter.in.websocket.CommunityThreadStompProbe.StompFrame;
import com.umc.product.community.adapter.in.websocket.CommunityThreadTwoInstanceFixture.Scenario;

final class CommunityThreadMutationE2EVerifier {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);
    private static final String EDITED_CONTENT = "수정된 답장입니다";
    private static final String REACTION = "👍";

    private final CommunityThreadTwoInstanceTopology topology;
    private final Scenario scenario;
    private final CommunityThreadFrameAwaiter awaiter;

    CommunityThreadMutationE2EVerifier(
        CommunityThreadTwoInstanceTopology topology,
        Scenario scenario,
        CommunityThreadFrameAwaiter awaiter
    ) {
        this.topology = topology;
        this.scenario = scenario;
        this.awaiter = awaiter;
    }

    void verify(
        CommunityThreadStompProbe owner,
        CommunityThreadStompProbe member,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        BlockingQueue<StompFrame> ownerErrors,
        Long messageId
    ) throws Exception {
        edit(member, ownerFrames, memberFrames, messageId);
        addAndRemoveReaction(owner, ownerFrames, memberFrames, messageId);
        read(owner, ownerFrames, memberFrames, messageId);
        tombstone(member, ownerFrames, memberFrames, messageId);
        rateLimitAndRecovery(owner, ownerFrames, ownerErrors, messageId);
    }

    private void edit(
        CommunityThreadStompProbe member,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        Long messageId
    ) throws Exception {
        UUID commandId = UUID.randomUUID();
        member.send(
            CommunityThreadE2EProtocol.edit(scenario.threadId(), messageId),
            commandId,
            Map.of("content", EDITED_CONTENT)
        );
        assertAck(memberFrames, commandId, "MESSAGE_EDIT", false);
        topology.relayOutbox();
        JsonNode updated = awaiter.awaitMessage(
            ownerFrames,
            "message.updated",
            EDITED_CONTENT,
            EVENT_TIMEOUT
        );
        assertThat(updated.at("/payload/message/editedAt").isTextual()).isTrue();
    }

    private void addAndRemoveReaction(
        CommunityThreadStompProbe owner,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        Long messageId
    ) throws Exception {
        UUID addCommandId = UUID.randomUUID();
        owner.send(
            CommunityThreadE2EProtocol.reaction(scenario.threadId(), messageId, "add"),
            addCommandId,
            Map.of("emoji", REACTION)
        );
        assertAck(ownerFrames, addCommandId, "REACTION_ADD", false);
        topology.relayOutbox();
        JsonNode added = awaitReaction(memberFrames, messageId);
        assertThat(added.at("/payload/reactions").size()).isEqualTo(1);
        assertThat(added.at("/payload/reactions/0/emoji").asText()).isEqualTo(REACTION);
        assertThat(added.at("/payload/reactions/0/count").asLong()).isEqualTo(1L);
        assertThat(added.at("/payload/reactions/0/reactedByMe").asBoolean()).isFalse();

        UUID removeCommandId = UUID.randomUUID();
        owner.send(
            CommunityThreadE2EProtocol.reaction(scenario.threadId(), messageId, "remove"),
            removeCommandId,
            Map.of("emoji", REACTION)
        );
        assertAck(ownerFrames, removeCommandId, "REACTION_REMOVE", false);
        topology.relayOutbox();
        assertThat(awaitReaction(memberFrames, messageId)
            .at("/payload/reactions").isEmpty()).isTrue();
    }

    private void read(
        CommunityThreadStompProbe owner,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        Long messageId
    ) throws Exception {
        UUID commandId = UUID.randomUUID();
        owner.send(
            CommunityThreadE2EProtocol.read(scenario.threadId()),
            commandId,
            Map.of("lastReadMessageId", messageId)
        );
        assertAck(ownerFrames, commandId, "READ_UPDATE", false);
        topology.relayOutbox();
        JsonNode event = awaiter.await(
            memberFrames,
            candidate -> "read.updated".equals(candidate.path("type").asText())
                && scenario.owner().memberId().equals(candidate.at("/payload/memberId").asLong()),
            "owner read.updated",
            EVENT_TIMEOUT
        );
        assertThat(event.at("/payload/lastReadMessageId").asLong()).isEqualTo(messageId);
    }

    private void tombstone(
        CommunityThreadStompProbe member,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> memberFrames,
        Long messageId
    ) throws Exception {
        UUID commandId = UUID.randomUUID();
        member.send(
            CommunityThreadE2EProtocol.delete(scenario.threadId(), messageId),
            commandId,
            Map.of()
        );
        assertAck(memberFrames, commandId, "MESSAGE_DELETE", false);
        topology.relayOutbox();
        JsonNode deleted = awaiter.awaitMessage(
            ownerFrames,
            "message.deleted",
            messageId,
            EVENT_TIMEOUT
        );
        assertThat(deleted.at("/payload/message/type").asText()).isEqualTo("SYSTEM");
        assertThat(deleted.at("/payload/message/content").asText()).isEqualTo("삭제된 메시지입니다.");
        assertThat(deleted.at("/payload/message/deletedAt").isTextual()).isTrue();
        assertThat(deleted.toString()).doesNotContain("chatRoomId").doesNotContain("\"roomId\"");
    }

    private void rateLimitAndRecovery(
        CommunityThreadStompProbe owner,
        BlockingQueue<StompFrame> ownerFrames,
        BlockingQueue<StompFrame> ownerErrors,
        Long messageId
    ) throws Exception {
        advanceBothClocks();

        UUID rejectedCommandId = null;
        for (int attempt = 0; attempt < 21; attempt++) {
            rejectedCommandId = UUID.randomUUID();
            owner.send(
                CommunityThreadE2EProtocol.read(scenario.threadId()),
                rejectedCommandId,
                Map.of("lastReadMessageId", messageId)
            );
        }
        JsonNode rejected = awaiter.awaitError(ownerErrors, rejectedCommandId, EVENT_TIMEOUT);
        assertThat(rejected.path("status").asInt()).isEqualTo(429);
        assertThat(rejected.path("code").asText()).isEqualTo("COMMON-429");
        assertThat(rejected.path("retryable").asBoolean()).isTrue();

        advanceBothClocks();
        UUID recoveryCommandId = UUID.randomUUID();
        owner.send(
            CommunityThreadE2EProtocol.read(scenario.threadId()),
            recoveryCommandId,
            Map.of("lastReadMessageId", messageId)
        );
        assertAck(ownerFrames, recoveryCommandId, "READ_UPDATE", true);
    }

    private void advanceBothClocks() {
        topology.appA().context().getBean(CommunityThreadE2EClock.class)
            .advance(Duration.ofSeconds(1));
        topology.appB().context().getBean(CommunityThreadE2EClock.class)
            .advance(Duration.ofSeconds(1));
    }

    private JsonNode awaitReaction(BlockingQueue<StompFrame> frames, Long messageId)
        throws Exception {
        return awaiter.await(
            frames,
            event -> "reaction.changed".equals(event.path("type").asText())
                && messageId.equals(event.at("/payload/messageId").asLong()),
            "reaction.changed messageId=" + messageId,
            EVENT_TIMEOUT
        );
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
}
