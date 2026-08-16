package com.umc.product.global.websocket.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.websocket.application.port.in.StompClientMessageIdResolver;

@DisplayName("StompClientMessageIdResolverRegistry")
class StompClientMessageIdResolverRegistryTest {

    private static final String DESTINATION = "/app/community/threads/10/messages";
    private static final UUID CLIENT_MESSAGE_ID =
        UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd");

    @Test
    @DisplayName("destination을 소유한 단일 resolver의 clientMessageId를 반환한다")
    void resolvesWithSingleOwner() {
        StompClientMessageIdResolver resolver = mock(StompClientMessageIdResolver.class);
        byte[] payload = new byte[0];
        given(resolver.supports(DESTINATION)).willReturn(true);
        given(resolver.resolve(payload)).willReturn(Optional.of(CLIENT_MESSAGE_ID));
        StompClientMessageIdResolverRegistry sut =
            new StompClientMessageIdResolverRegistry(List.of(resolver));

        Optional<UUID> result = sut.resolve(DESTINATION, payload);

        assertThat(result).contains(CLIENT_MESSAGE_ID);
    }

    @Test
    @DisplayName(
        "resolver 장애는 원래 프레임 처리를 방해하지 않도록 빈 correlation으로 축소한다"
    )
    void resolverFailureReturnsEmpty() {
        StompClientMessageIdResolver resolver = mock(StompClientMessageIdResolver.class);
        given(resolver.supports(DESTINATION)).willThrow(new IllegalStateException("resolver failure"));
        StompClientMessageIdResolverRegistry sut =
            new StompClientMessageIdResolverRegistry(List.of(resolver));

        Optional<UUID> result = sut.resolve(DESTINATION, new byte[0]);

        assertThat(result).isEmpty();
    }
}
