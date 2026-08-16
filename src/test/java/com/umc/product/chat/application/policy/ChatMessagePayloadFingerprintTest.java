package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.domain.MessageContentType;

@DisplayName("ChatMessagePayloadFingerprint")
class ChatMessagePayloadFingerprintTest {

    private static final UUID CLIENT_ID = UUID.fromString("750bc9b1-0c72-4b57-9d14-9278de74a885");

    @Test
    @DisplayName("중복 mention을 정규화한 동일 payload는 같은 fingerprint다")
    void sameCanonicalPayload() {
        CreateChatMessageCommand first = command("캡션", List.of("b", "a"), List.of(30L, 20L, 30L), 90L);
        CreateChatMessageCommand second = command("캡션", List.of("b", "a"), List.of(20L, 30L), 90L);

        assertThat(ChatMessagePayloadFingerprint.from(first))
            .isEqualTo(ChatMessagePayloadFingerprint.from(second));
    }

    @Test
    @DisplayName("ordered file, exact content, reply 중 하나라도 다르면 fingerprint가 다르다")
    void differentCanonicalPayload() {
        CreateChatMessageCommand baseline = command("캡션", List.of("b", "a"), List.of(20L), 90L);

        assertThat(ChatMessagePayloadFingerprint.from(command("캡션", List.of("a", "b"), List.of(20L), 90L)))
            .isNotEqualTo(ChatMessagePayloadFingerprint.from(baseline));
        assertThat(ChatMessagePayloadFingerprint.from(command("캡션 ", List.of("b", "a"), List.of(20L), 90L)))
            .isNotEqualTo(ChatMessagePayloadFingerprint.from(baseline));
        assertThat(ChatMessagePayloadFingerprint.from(command("캡션", List.of("b", "a"), List.of(20L), 91L)))
            .isNotEqualTo(ChatMessagePayloadFingerprint.from(baseline));
    }

    private CreateChatMessageCommand command(
        String content,
        List<String> files,
        List<Long> mentions,
        Long replyToId
    ) {
        return new CreateChatMessageCommand(
            1L,
            10L,
            CLIENT_ID,
            MessageContentType.IMAGE,
            content,
            files,
            mentions,
            replyToId
        );
    }
}
