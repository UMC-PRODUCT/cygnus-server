package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.ApiRateLimitProperties;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RecruitingCredentialGraphQlRateLimitInterceptorTest {

    @Mock
    WebGraphQlInterceptor.Chain chain;

    @Mock
    WebGraphQlResponse downstreamResponse;

    RecruitingCredentialGraphQlRateLimitInterceptor sut;

    @BeforeEach
    void setUp() {
        ApiRateLimitProperties properties = ApiRateLimitProperties.defaults();
        sut = new RecruitingCredentialGraphQlRateLimitInterceptor(
            new RateLimitBucketRegistry(properties),
            new ApiRateLimitMetrics(new SimpleMeterRegistry())
        );
    }

    @Test
    @DisplayName("credential이 아닌 GraphQL 요청은 전용 rate limit bucket을 사용하지 않는다")
    void passNonCredentialOperation() {
        WebGraphQlRequest request = request("query { recruitingApplicationForms(input: {gisuId: 1, schoolId: 2}) { formId } }");
        given(chain.next(request)).willReturn(Mono.just(downstreamResponse));

        WebGraphQlResponse result = sut.intercept(request, chain).block();

        assertThat(result).isSameAs(downstreamResponse);
        then(chain).should().next(request);
    }

    @Test
    @DisplayName("alias와 fragment로 묶은 credential 다중 호출도 실행 전에 제한한다")
    void blockBatchedCredentialFieldsInFragment() {
        WebGraphQlRequest request = request("""
            query CredentialLookup {
              ...CredentialFields
            }
            fragment CredentialFields on Query {
              first: recruitingApplicationByCredential(input: {email: "a@example.com", applicationKey: "A1B2C3"}) {
                applicationId
              }
              second: recruitingApplicationByCredential(input: {email: "a@example.com", applicationKey: "A1B2C3"}) {
                applicationId
              }
            }
            """);

        WebGraphQlResponse result = sut.intercept(request, chain).block();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst().getExtensions())
            .containsEntry("code", CommonErrorCode.TOO_MANY_REQUESTS.getCode());
        then(chain).shouldHaveNoInteractions();
    }

    private WebGraphQlRequest request(String document) {
        return new WebGraphQlRequest(
            URI.create("http://localhost/graphql"),
            HttpHeaders.EMPTY,
            new LinkedMultiValueMap<>(),
            new InetSocketAddress("127.0.0.1", 12345),
            Map.of(),
            new DefaultGraphQlRequest(document),
            "test-request",
            Locale.KOREA
        );
    }
}
