package com.umc.product.global.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClientContextProperties.class)
public class ClientContextConfig {

    @Bean
    public ClientOriginRegistry clientOriginRegistry(ClientContextProperties properties) {
        return new ClientOriginRegistry(properties);
    }

    @Bean
    public ClientRequestClassifier clientRequestClassifier(ClientOriginRegistry originRegistry) {
        return new ClientRequestClassifier(originRegistry);
    }
}
