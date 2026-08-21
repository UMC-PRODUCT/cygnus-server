package com.umc.product.global.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.umc.product.demoday.adapter.in.web.support.CurrentDemodayParticipantArgumentResolver;
import com.umc.product.global.client.ClientContextConfig;
import com.umc.product.global.logging.OperationalMetricsConfig;
import com.umc.product.global.ratelimit.ApiRateLimitInterceptor;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.resolver.CurrentMemberArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@Import({
    ClientContextConfig.class,
    OperationalMetricsConfig.class,
    CurrentMemberSecurityConfig.class
})
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;
    private final ObjectProvider<CurrentDemodayParticipantArgumentResolver> currentDemodayParticipantArgumentResolverProvider;
    private final LoggingInterceptor loggingInterceptor;
    private final ObjectProvider<ApiRateLimitInterceptor> apiRateLimitInterceptorProvider;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
        currentDemodayParticipantArgumentResolverProvider.ifAvailable(resolvers::add);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(SecurityPathConfig.SCALAR_ENTRY_PATH, "/docs/scalar.html");
        registry.addRedirectViewController(SecurityPathConfig.SCALAR_ENTRY_SLASH_PATH, "/docs/scalar.html");
        registry.addRedirectViewController(
            SecurityPathConfig.ASYNCAPI_ENTRY_PATH,
            SecurityPathConfig.ASYNCAPI_HTML_PATH
        );
        registry.addRedirectViewController(
            SecurityPathConfig.ASYNCAPI_ENTRY_SLASH_PATH,
            SecurityPathConfig.ASYNCAPI_HTML_PATH
        );
        registry.addRedirectViewController("/docs/catalog/error", "/docs/catalog/error/index.html");
        registry.addRedirectViewController("/docs/catalog/error/", "/docs/catalog/error/index.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(SecurityPathConfig.loggingExcludedPaths());
        apiRateLimitInterceptorProvider.ifAvailable(apiRateLimitInterceptor ->
            registry.addInterceptor(apiRateLimitInterceptor)
                .addPathPatterns("/**")
        );
    }
}
