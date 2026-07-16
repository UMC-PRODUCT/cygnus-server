package com.umc.product.recruiting.adapter.in.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RecruitingWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<RecruitingCredentialRestRateLimitInterceptor> interceptorProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        interceptorProvider.ifAvailable(interceptor -> registry.addInterceptor(interceptor)
            .addPathPatterns(
                "/api/v1/recruiting/public/applications",
                "/api/v1/recruiting/public/applications/**"
            ));
    }
}
