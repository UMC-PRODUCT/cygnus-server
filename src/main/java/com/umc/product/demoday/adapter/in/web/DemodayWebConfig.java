package com.umc.product.demoday.adapter.in.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DemodayWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<DemodayGuestRateLimitInterceptor> interceptorProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        interceptorProvider.ifAvailable(interceptor -> registry.addInterceptor(interceptor)
            .addPathPatterns("/api/v1/demoday/polls/*/participations/guest"));
    }
}
