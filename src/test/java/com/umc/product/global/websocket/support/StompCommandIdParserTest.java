package com.umc.product.global.websocket.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

@DisplayName("StompCommandIdParser")
class StompCommandIdParserTest {

    @Test
    @DisplayName("canonical lowercase UUID x-command-id만 correlation ID로 해석한다")
    void parseCanonicalCommandId() {
        UUID commandId = UUID.fromString("b108f0c7-e244-4c9d-a57a-6e5bb8e94e89");
        StompHeaderAccessor accessor = accessor(commandId.toString());

        assertThat(StompCommandIdParser.parse(accessor)).isEqualTo(commandId);
    }

    @Test
    @DisplayName("대문자, 비정규 UUID, 누락 헤더는 correlation ID로 사용하지 않는다")
    void rejectNonCanonicalCommandId() {
        assertThat(StompCommandIdParser.parse(accessor(
            "B108F0C7-E244-4C9D-A57A-6E5BB8E94E89"
        ))).isNull();
        assertThat(StompCommandIdParser.parse(accessor("not-a-uuid"))).isNull();
        assertThat(StompCommandIdParser.parse(accessor(null))).isNull();
    }

    private StompHeaderAccessor accessor(String commandId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        if (commandId != null) {
            accessor.setNativeHeader(StompCommandIdParser.COMMAND_ID_HEADER, commandId);
        }
        return accessor;
    }
}
