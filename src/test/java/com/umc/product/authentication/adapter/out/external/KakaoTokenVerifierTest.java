package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("KakaoTokenVerifier")
class KakaoTokenVerifierTest {

    @Test
    @DisplayName("Kakao OIDC ID Token을 JWKS 공개키로 검증하고 OAuthAttributes로 변환한다")
    void verify_kakao_oidc_id_token() {
        TestFixture fixture = new TestFixture();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        fixture.expectKakaoJwks("kakao-kid", keyPair);
        String idToken = OidcTokenTestSupport.signedIdToken(
            keyPair,
            "kakao-kid",
            "https://kauth.kakao.com",
            "kakao-rest-api-key",
            "123456789",
            Map.of("email", "kakao@example.com", "nickname", "카카오")
        );

        OAuthAttributes attrs = fixture.verifier.verify(idToken);

        assertThat(attrs.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(attrs.providerId()).isEqualTo("123456789");
        assertThat(attrs.email()).isEqualTo("kakao@example.com");
        fixture.server.verify();
    }

    @Test
    @DisplayName("Kakao 인가 코드 교환 응답에 id_token이 있으면 userinfo 호출 없이 OIDC 검증을 사용한다")
    void authorization_code_uses_id_token_when_present() {
        TestFixture fixture = new TestFixture();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        fixture.expectTokenExchangeWithIdToken(keyPair);
        fixture.expectKakaoJwks("kakao-kid", keyPair);
        fixture.server.expect(never(), requestTo("https://kapi.kakao.com/v2/user/me"));

        OAuthAttributes attrs = fixture.verifier.verifyAuthorizationCode(
            "authorization-code",
            "https://app.example.com/oauth/kakao/callback"
        );

        assertThat(attrs.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(attrs.providerId()).isEqualTo("123456789");
        assertThat(attrs.email()).isEqualTo("kakao@example.com");
        fixture.server.verify();
    }

    private static class TestFixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final KakaoTokenVerifier verifier;

        TestFixture() {
            CacheUseCase cacheUseCase = new FakeCacheUseCase();
            OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
                builder.build(),
                new ObjectMapper(),
                cacheUseCase
            );
            verifier = new KakaoTokenVerifier(
                builder.build(),
                new KakaoOAuthProperties(
                    "kakao-rest-api-key",
                    "",
                    "",
                    List.of("https://app.example.com/oauth/kakao/callback"),
                    new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
                ),
                resolver
            );
        }

        void expectKakaoJwks(String kid, KeyPair keyPair) {
            server.expect(once(), requestTo("https://kauth.kakao.com/.well-known/jwks.json"))
                .andRespond(withSuccess(
                    OidcTokenTestSupport.jwks(kid, (RSAPublicKey) keyPair.getPublic()),
                    MediaType.APPLICATION_JSON
                ));
        }

        void expectTokenExchangeWithIdToken(KeyPair keyPair) {
            String idToken = OidcTokenTestSupport.signedIdToken(
                keyPair,
                "kakao-kid",
                "https://kauth.kakao.com",
                "kakao-rest-api-key",
                "123456789",
                Map.of("email", "kakao@example.com")
            );
            server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                    {
                      "access_token": "legacy-access-token",
                      "token_type": "bearer",
                      "refresh_token": "refresh-token",
                      "expires_in": 21599,
                      "scope": "openid account_email",
                      "id_token": "%s"
                    }
                    """.formatted(idToken), MediaType.APPLICATION_JSON));
        }
    }

    private static class FakeCacheUseCase implements CacheUseCase {

        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
            Object value = storage.get(spec.namespace().value() + ":" + key.value());
            if (value == null) {
                return new CacheLookup.Miss<>();
            }
            return new CacheLookup.Hit<>(spec.valueType().cast(value));
        }

        @Override
        public <T> void put(CacheSpec<T> spec, CacheKey key, T value) {
            storage.put(spec.namespace().value() + ":" + key.value(), value);
        }

        @Override
        public void evict(CacheNamespace namespace, CacheKey key) {
            storage.remove(namespace.value() + ":" + key.value());
        }
    }
}
