package com.umc.product.global.security.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.umc.product.global.security.annotation.Public;

class PublicEndpointCollectorTest {

    @Test
    @DisplayName("HTTP method가 없는 Public endpoint는 모든 method matcher로 수집한다")
    void public_endpoint_without_method() throws Exception {
        new PublicEndpointCollector();
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo info = RequestMappingInfo.paths("/public-all").build();
        Method method = PublicController.class.getDeclaredMethod("all");
        given(mapping.getHandlerMethods()).willReturn(Map.of(info, new HandlerMethod(new PublicController(), method)));

        assertThat(PublicEndpointCollector.collectPublicEndpoints(mapping))
            .containsExactly(new PublicEndpointCollector.EndpointMatcher(null, "/public-all"));
    }

    @Test
    @DisplayName("Public이 아닌 endpoint는 제외하고 method가 있으면 함께 수집한다")
    void method_public_and_private_endpoint() throws Exception {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo publicInfo = RequestMappingInfo.paths("/public-get").methods(
            org.springframework.web.bind.annotation.RequestMethod.GET
        ).build();
        RequestMappingInfo privateInfo = RequestMappingInfo.paths("/private").build();
        Method publicMethod = PublicController.class.getDeclaredMethod("get");
        Method privateMethod = PrivateController.class.getDeclaredMethod("get");
        given(mapping.getHandlerMethods()).willReturn(Map.of(
            publicInfo, new HandlerMethod(new PublicController(), publicMethod),
            privateInfo, new HandlerMethod(new PrivateController(), privateMethod)
        ));

        assertThat(PublicEndpointCollector.collectPublicEndpoints(mapping))
            .containsExactly(new PublicEndpointCollector.EndpointMatcher(HttpMethod.GET, "/public-get"));
    }

    static class PublicController {
        @Public
        void all() {
        }

        @Public
        @GetMapping
        void get() {
        }
    }

    static class PrivateController {
        void get() {
        }
    }
}
