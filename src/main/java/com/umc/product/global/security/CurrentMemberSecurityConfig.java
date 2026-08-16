package com.umc.product.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CurrentMemberSecurityConfig {

    @Bean
    public CurrentMemberProvider currentMemberProvider() {
        return new CurrentMemberProvider();
    }
}
