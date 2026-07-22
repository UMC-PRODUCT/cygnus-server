package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import org.springframework.test.util.ReflectionTestUtils;
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
        WebGraphQlRequest request = request(
            "query { publicRecruitingRounds(input: {gisuId: 1}) { seasonId } }"
        );
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

    @Test
    @DisplayName("익명 지원서 철회 mutation도 credential 요청으로 제한한다")
    void limitAnonymousCancellationMutation() {
        WebGraphQlRequest request = request("""
            mutation {
              first: cancelAnonymousRecruitingApplication(
                input: {email: "a@example.com", applicationKey: "A1B2C3"}
              ) { applicationId }
              second: cancelAnonymousRecruitingApplication(
                input: {email: "a@example.com", applicationKey: "A1B2C3"}
              ) { applicationId }
            }
            """);

        WebGraphQlResponse result = sut.intercept(request, chain).block();

        assertThat(result).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst().getExtensions())
            .containsEntry("code", CommonErrorCode.TOO_MANY_REQUESTS.getCode());
        then(chain).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("문법 오류와 operation 선택이 모호한 요청은 credential 제한을 건너뛴다")
    void passInvalidOrAmbiguousDocument() {
        WebGraphQlRequest invalid = request("query {");
        WebGraphQlRequest ambiguous = request("query First { __typename } query Second { __typename }");
        given(chain.next(invalid)).willReturn(Mono.just(downstreamResponse));
        given(chain.next(ambiguous)).willReturn(Mono.just(downstreamResponse));

        assertThat(sut.intercept(invalid, chain).block()).isSameAs(downstreamResponse);
        assertThat(sut.intercept(ambiguous, chain).block()).isSameAs(downstreamResponse);
    }

    @Test
    @DisplayName("operationName으로 고른 단일 credential 요청은 실행하고 rate limit header를 제공한다")
    void allowSelectedCredentialOperation() {
        WebGraphQlRequest request = request(
            "query Public { __typename } "
                + "query Credential { recruitingApplicationByCredential(input: {email: \"a@example.com\", "
                + "applicationKey: \"A1B2C3\"}) { applicationId } }",
            "Credential",
            null
        );
        given(chain.next(request)).willReturn(Mono.just(downstreamResponse));
        given(downstreamResponse.getResponseHeaders()).willReturn(new HttpHeaders());

        WebGraphQlResponse result = sut.intercept(request, chain).block();

        assertThat(result).isSameAs(downstreamResponse);
        assertThat(result.getResponseHeaders()).containsKey("X-RateLimit-Limit");
    }

    @Test
    @DisplayName("inline fragment의 credential field와 원격 주소가 없는 요청도 제한한다")
    void countInlineFragmentWithUnknownClientIp() {
        WebGraphQlRequest request = request("""
            query {
              ... on Query {
                first: recruitingApplicationByCredential(input: {email: "a@example.com", applicationKey: "A1B2C3"}) {
                  applicationId
                }
                second: recruitingApplicationByCredential(input: {email: "a@example.com", applicationKey: "A1B2C3"}) {
                  applicationId
                }
              }
            }
            """, null, null);

        WebGraphQlResponse result = sut.intercept(request, chain).block();

        assertThat(result.getErrors()).hasSize(1);
        then(chain).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DNS 해석 전 원격 주소는 host 문자열을 rate limit key로 사용한다")
    void useHostStringForUnresolvedClientAddress() {
        WebGraphQlRequest request = request(
            "query { recruitingApplicationByCredential(input: {email: \"a@example.com\", "
                + "applicationKey: \"A1B2C3\"}) { applicationId } }",
            null,
            InetSocketAddress.createUnresolved("unresolved.test", 12345)
        );
        given(chain.next(request)).willReturn(Mono.just(downstreamResponse));
        given(downstreamResponse.getResponseHeaders()).willReturn(new HttpHeaders());

        assertThat(sut.intercept(request, chain).block()).isSameAs(downstreamResponse);
    }

    @Test
    @DisplayName("선택 집합이 없으면 credential field 수는 0이다")
    void countNullSelectionSetAsZero() {
        Integer count = ReflectionTestUtils.invokeMethod(
            sut,
            "countCredentialFields",
            null,
            Map.of(),
            Set.of()
        );

        assertThat(count).isZero();
    }

    private WebGraphQlRequest request(String document) {
        return request(document, null, new InetSocketAddress("127.0.0.1", 12345));
    }

    private WebGraphQlRequest request(
        String document,
        String operationName,
        InetSocketAddress remoteAddress
    ) {
        return new WebGraphQlRequest(
            URI.create("http://localhost/graphql"),
            HttpHeaders.EMPTY,
            new LinkedMultiValueMap<>(),
            remoteAddress,
            Map.of(),
            new DefaultGraphQlRequest(document, operationName, Map.of(), Map.of()),
            "test-request",
            Locale.KOREA
        );
    }
}
