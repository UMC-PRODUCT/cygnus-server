package com.umc.product.community.application.port.in.realtime.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("Community thread realtime event envelope")
class CommunityThreadRealtimeEventTest {

    @Test
    @DisplayName("event envelope은 stable eventId와 string threadId 및 typed payload만 노출한다")
    void serializesStablePublicContractWithoutRawChatRoomId() throws Exception {
        UUID eventId = UUID.fromString("864f98e4-1a30-4d57-b685-240c246b643b");
        CommunityThreadRealtimeEvent<?> event = CommunityThreadRealtimeEvent.of(
            eventId,
            CommunityThreadRealtimeEventType.READ_UPDATED,
            11L,
            Instant.parse("2026-07-18T00:00:00Z"),
            new CommunityThreadRealtimePayload.ReadUpdated(20L, 900L)
        );

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(event);

        assertThat(json.path("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(json.path("type").asText()).isEqualTo("read.updated");
        assertThat(json.path("threadId").isTextual()).isTrue();
        assertThat(json.path("threadId").asText()).isEqualTo("11");
        assertThat(json.path("payload").path("memberId").asLong()).isEqualTo(20L);
        assertThat(json.has("chatRoomId")).isFalse();
        assertThat(json.path("payload").has("chatRoomId")).isFalse();
    }
}
