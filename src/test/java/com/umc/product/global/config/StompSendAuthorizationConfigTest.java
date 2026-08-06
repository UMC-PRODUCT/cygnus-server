package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.umc.product.global.websocket.application.port.in.StompClientMessageIdResolver;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;

@DisplayName("StompSendAuthorizationConfig")
class StompSendAuthorizationConfigTest {

    private static final String DESTINATION = "/app/community/threads/10/messages";

    private final StompSendAuthorizationConfig sut = new StompSendAuthorizationConfig();

    @Test
    @DisplayName("resolver가 없는 slice에도 빈 registry bean을 제공한다")
    void createsEmptyResolverRegistryForSlice() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StompClientMessageIdResolver> resolvers = mock(ObjectProvider.class);
        given(resolvers.orderedStream()).willReturn(Stream.empty());

        StompClientMessageIdResolverRegistry registry =
            sut.stompClientMessageIdResolverRegistry(resolvers);

        assertThat(registry.resolve(DESTINATION, new byte[0])).isEmpty();
    }

    @Test
    @DisplayName("production에 등록된 domain resolver를 registry에 전달한다")
    void registersAvailableDomainResolver() {
        UUID clientMessageId = UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd");
        byte[] payload = new byte[0];
        StompClientMessageIdResolver resolver = mock(StompClientMessageIdResolver.class);
        given(resolver.supports(DESTINATION)).willReturn(true);
        given(resolver.resolve(payload)).willReturn(Optional.of(clientMessageId));
        @SuppressWarnings("unchecked")
        ObjectProvider<StompClientMessageIdResolver> resolvers = mock(ObjectProvider.class);
        given(resolvers.orderedStream()).willReturn(Stream.of(resolver));

        StompClientMessageIdResolverRegistry registry =
            sut.stompClientMessageIdResolverRegistry(resolvers);

        assertThat(registry.resolve(DESTINATION, payload)).contains(clientMessageId);
    }
}
