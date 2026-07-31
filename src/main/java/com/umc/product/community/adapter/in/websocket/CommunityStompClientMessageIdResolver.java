package com.umc.product.community.adapter.in.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.global.websocket.application.port.in.StompClientMessageIdResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityStompClientMessageIdResolver implements StompClientMessageIdResolver {

    private static final String CLIENT_MESSAGE_ID = "clientMessageId";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String destination) {
        return CommunityStompDestinationParser.parseSend(destination)
            .map(CommunityStompDestinationParser.SendDestination::command)
            .filter(CommunityStompCommandType.MESSAGE_CREATE::equals)
            .isPresent();
    }

    @Override
    public Optional<UUID> resolve(Object payload) {
        if (payload instanceof CreateCommunityThreadMessageRequest request) {
            return Optional.ofNullable(CommunityStompUuid.parseOrNull(request.clientMessageId()));
        }

        String json = switch (payload) {
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            case String text -> text;
            default -> null;
        };
        if (json == null) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode value = root == null ? null : root.get(CLIENT_MESSAGE_ID);
            return value != null && value.isTextual()
                ? Optional.ofNullable(CommunityStompUuid.parseOrNull(value.textValue()))
                : Optional.empty();
        } catch (JsonProcessingException ignored) {
            return Optional.empty();
        }
    }
}
