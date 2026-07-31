package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.support.IntegrationTestSupport;

@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
@DisplayName("Community thread retained REST/OpenAPI 계약")
class CommunityThreadRestOpenApiContractIntegrationTest extends IntegrationTestSupport {

    private static final String BASE = "/api/v1/community";
    private static final Map<String, Set<RequestMethod>> RETAINED_ROUTES = retainedRoutes();

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("runtime handler mapping은 section 5.1 retained route만 정확히 등록한다")
    void runtimeHandlerMapping_containsExactlyRetainedRoutes() {
        Map<String, Set<RequestMethod>> actual = handlerMapping.getHandlerMethods().keySet().stream()
            .flatMap(mapping -> mapping.getPatternValues().stream()
                .filter(path -> path.startsWith(BASE))
                .map(path -> Map.entry(path, methods(mapping))))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> {
                    var merged = new java.util.HashSet<>(left);
                    merged.addAll(right);
                    return Set.copyOf(merged);
                },
                TreeMap::new
            ));

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(RETAINED_ROUTES);
    }

    @Test
    @DisplayName("runtime OpenAPI JSON은 retained method를 노출하고 금지 mutation을 노출하지 않는다")
    void runtimeOpenApi_exposesOnlyRetainedMethods() throws Exception {
        String openApiJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/docs-json")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode paths = objectMapper.readTree(openApiJson).path("paths");

        assertThat(fieldNames(paths).stream()
            .filter(path -> path.startsWith(BASE))
            .toList())
            .containsExactlyInAnyOrderElementsOf(RETAINED_ROUTES.keySet());
        RETAINED_ROUTES.forEach((path, requestMethods) ->
            assertThat(fieldNames(paths.path(path)))
                .containsExactlyInAnyOrderElementsOf(requestMethods.stream()
                    .map(method -> method.name().toLowerCase(Locale.ROOT))
                    .toList())
        );
    }

    @ParameterizedTest
    @MethodSource("forbiddenRoutes")
    @DisplayName("message/reaction/read REST mutation은 명시적으로 존재하지 않는다")
    void forbiddenMutationRoutes_areAbsent(HttpMethod method, String path, HttpStatus expectedStatus)
        throws Exception {
        mockMvc.perform(request(method, path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is(expectedStatus.value()));
    }

    private static Set<RequestMethod> methods(RequestMappingInfo mapping) {
        return Set.copyOf(mapping.getMethodsCondition().getMethods());
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return List.copyOf(names);
    }

    private static Stream<Arguments> forbiddenRoutes() {
        return Stream.of(
            Arguments.of(
                HttpMethod.POST,
                BASE + "/threads/42/messages",
                HttpStatus.METHOD_NOT_ALLOWED
            ),
            Arguments.of(HttpMethod.PATCH, BASE + "/threads/42/messages/500", HttpStatus.NOT_FOUND),
            Arguments.of(HttpMethod.DELETE, BASE + "/threads/42/messages/500", HttpStatus.NOT_FOUND),
            Arguments.of(
                HttpMethod.POST,
                BASE + "/threads/42/messages/500/reactions/add",
                HttpStatus.NOT_FOUND
            ),
            Arguments.of(
                HttpMethod.DELETE,
                BASE + "/threads/42/messages/500/reactions/remove",
                HttpStatus.NOT_FOUND
            ),
            Arguments.of(HttpMethod.PATCH, BASE + "/threads/42/read", HttpStatus.NOT_FOUND)
        );
    }

    private static Map<String, Set<RequestMethod>> retainedRoutes() {
        Map<String, Set<RequestMethod>> routes = new LinkedHashMap<>();
        routes.put(BASE + "/threads", Set.of(RequestMethod.GET, RequestMethod.POST));
        routes.put(
            BASE + "/threads/{threadId}",
            Set.of(RequestMethod.GET, RequestMethod.PATCH, RequestMethod.DELETE)
        );
        routes.put(BASE + "/threads/{threadId}/pin", Set.of(RequestMethod.POST, RequestMethod.DELETE));
        routes.put(BASE + "/threads/{threadId}/mute", Set.of(RequestMethod.POST, RequestMethod.DELETE));
        routes.put(BASE + "/threads/{threadId}/members", Set.of(RequestMethod.GET));
        routes.put(BASE + "/threads/{threadId}/invitable", Set.of(RequestMethod.GET));
        routes.put(BASE + "/threads/{threadId}/invite", Set.of(RequestMethod.POST));
        routes.put(
            BASE + "/threads/{threadId}/members/{memberId}",
            Set.of(RequestMethod.DELETE)
        );
        routes.put(BASE + "/threads/{threadId}/leave", Set.of(RequestMethod.POST));
        routes.put(
            BASE + "/threads/{threadId}/members/{memberId}/role",
            Set.of(RequestMethod.PATCH)
        );
        routes.put(BASE + "/threads/{threadId}/messages", Set.of(RequestMethod.GET));
        routes.put(BASE + "/messages/{messageId}/report", Set.of(RequestMethod.POST));
        routes.put(BASE + "/admin/thread-message-reports", Set.of(RequestMethod.GET));
        return Map.copyOf(routes);
    }
}
