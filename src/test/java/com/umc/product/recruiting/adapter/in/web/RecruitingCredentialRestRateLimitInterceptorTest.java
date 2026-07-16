package com.umc.product.recruiting.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.ApiRateLimitProperties;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;
import com.umc.product.global.response.ApiErrorResponseWriter;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RecruitingCredentialRestRateLimitInterceptorTest {

    @Test
    @DisplayName("같은 IP의 credential 요청은 endpoint와 관계없이 전용 한도를 공유한다")
    void blockCredentialRequestsByClientIp() throws Exception {
        CredentialController controller = new CredentialController();
        MockMvc mockMvc = mockMvc(controller);

        mockMvc.perform(post("/api/v1/recruiting/public/applications/lookup")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.1");
                    return request;
                }))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "5"));

        mockMvc.perform(post("/api/v1/recruiting/public/applications/submit")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.1");
                    return request;
                }))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value(CommonErrorCode.TOO_MANY_REQUESTS.getCode()));

        mockMvc.perform(post("/api/v1/recruiting/public/applications/submit")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.2");
                    return request;
                }))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("지원서 생성 요청은 credential 전용 한도를 소비하지 않고 수정 요청은 소비한다")
    void limitOnlyCredentialRequests() throws Exception {
        CredentialController controller = new CredentialController();
        MockMvc mockMvc = mockMvc(controller);

        mockMvc.perform(post("/api/v1/recruiting/public/applications"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/recruiting/public/applications"))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/recruiting/public/applications"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "5"));

        mockMvc.perform(put("/api/v1/recruiting/public/applications"))
            .andExpect(status().isTooManyRequests());
    }

    private static MockMvc mockMvc(CredentialController controller) {
        ApiRateLimitProperties properties = ApiRateLimitProperties.defaults();
        RecruitingCredentialRestRateLimitInterceptor interceptor = new RecruitingCredentialRestRateLimitInterceptor(
            new RateLimitBucketRegistry(properties),
            new ApiErrorResponseWriter(new ObjectMapper()),
            new ApiRateLimitMetrics(new SimpleMeterRegistry())
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(interceptor)
            .build();
    }

    @Controller
    @ResponseBody
    @RequestMapping("/api/v1/recruiting/public/applications")
    private static class CredentialController {

        private final AtomicInteger invocations = new AtomicInteger();

        @PostMapping
        void create() {
            invocations.incrementAndGet();
        }

        @PostMapping("/lookup")
        void lookup() {
            invocations.incrementAndGet();
        }

        @PostMapping("/submit")
        void submit() {
            invocations.incrementAndGet();
        }

        @PutMapping
        void update() {
            invocations.incrementAndGet();
        }
    }
}
