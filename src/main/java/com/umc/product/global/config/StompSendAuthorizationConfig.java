package com.umc.product.global.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.global.websocket.application.port.in.StompClientMessageIdResolver;
import com.umc.product.global.websocket.application.port.in.StompSendAuthorizer;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;
import com.umc.product.global.websocket.application.service.StompSendAuthorizerRegistry;

@Configuration(proxyBeanMethods = false)
public class StompSendAuthorizationConfig {

    @Bean
    public StompClientMessageIdResolverRegistry stompClientMessageIdResolverRegistry(
        ObjectProvider<StompClientMessageIdResolver> resolvers
    ) {
        return new StompClientMessageIdResolverRegistry(resolvers.orderedStream().toList());
    }

    @Bean
    public StompSendAuthorizerRegistry stompSendAuthorizerRegistry(
        ObjectProvider<StompSendAuthorizer> authorizers
    ) {
        return new StompSendAuthorizerRegistry(authorizers.orderedStream().toList());
    }
}
