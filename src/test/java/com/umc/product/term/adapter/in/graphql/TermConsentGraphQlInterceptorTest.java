package com.umc.product.term.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.config.TermConsentEnforcementProperties;

import reactor.core.publisher.Mono;

class TermConsentGraphQlInterceptorTest {

    private final CurrentMemberProvider currentMemberProvider = mock(CurrentMemberProvider.class);
    private final OperationalMetrics operationalMetrics = mock(OperationalMetrics.class);
    private final WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);

    @Test
    @DisplayName("미동의 사용자의 GraphQL 요청은 resolver 실행 전에 TERMS-0012로 차단한다")
    void blockBeforeResolverExecution() {
        WebGraphQlRequest request = request("query { projects { id } }");
        given(currentMemberProvider.getNullableCurrentMember()).willReturn(principal(false));
        TermConsentGraphQlInterceptor sut = interceptor(true);

        WebGraphQlResponse response = sut.intercept(request, chain).block();

        assertThat(response).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst().getExtensions())
            .containsEntry("code", "TERMS-0012")
            .containsEntry("httpStatus", 403);
        then(chain).shouldHaveNoInteractions();
        then(operationalMetrics).should()
            .recordSecurityEvent("terms", "reconsent_enforcement", "blocked_graphql");
    }

    @Test
    @DisplayName("익명 공개 Query는 기존 GraphQL 처리로 넘긴다")
    void passAnonymousPublicQuery() {
        WebGraphQlRequest request = request("query { publicRecruitingRounds { seasonId } }");
        WebGraphQlResponse downstream = mock(WebGraphQlResponse.class);
        given(currentMemberProvider.getNullableCurrentMember()).willReturn(null);
        given(chain.next(request)).willReturn(Mono.just(downstream));

        WebGraphQlResponse response = interceptor(true).intercept(request, chain).block();

        assertThat(response).isSameAs(downstream);
        then(chain).should().next(request);
    }

    @Test
    @DisplayName("동의 완료 사용자와 feature flag 비활성 요청은 기존 GraphQL 처리로 넘긴다")
    void passAgreedOrDisabledRequest() {
        WebGraphQlRequest agreedRequest = request("query { projects { id } }");
        WebGraphQlResponse agreedResponse = mock(WebGraphQlResponse.class);
        given(currentMemberProvider.getNullableCurrentMember()).willReturn(principal(true));
        given(chain.next(agreedRequest)).willReturn(Mono.just(agreedResponse));
        assertThat(interceptor(true).intercept(agreedRequest, chain).block()).isSameAs(agreedResponse);

        WebGraphQlRequest disabledRequest = request("query { projects { id } }");
        WebGraphQlResponse disabledResponse = mock(WebGraphQlResponse.class);
        given(chain.next(disabledRequest)).willReturn(Mono.just(disabledResponse));
        assertThat(interceptor(false).intercept(disabledRequest, chain).block()).isSameAs(disabledResponse);
    }

    private TermConsentGraphQlInterceptor interceptor(boolean enabled) {
        return new TermConsentGraphQlInterceptor(
            new TermConsentEnforcementProperties(enabled),
            currentMemberProvider,
            operationalMetrics
        );
    }

    private MemberPrincipal principal(boolean requiredTermsAgreed) {
        return MemberPrincipal.builder()
            .memberId(100L)
            .requiredTermsAgreed(requiredTermsAgreed)
            .build();
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
