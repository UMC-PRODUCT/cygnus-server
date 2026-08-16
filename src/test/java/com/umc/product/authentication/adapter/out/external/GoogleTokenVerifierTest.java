package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("GoogleTokenVerifier")
class GoogleTokenVerifierTest {

    @Test
    @DisplayName("Google OIDC ID Token을 JWKS 공개키로 검증하고 OAuthAttributes로 변환한다")
    void verify_google_oidc_id_token() {
        TestFixture fixture = new TestFixture();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        fixture.expectGoogleJwks("google-kid", keyPair);
        String idToken = OidcTokenTestSupport.signedIdToken(
            keyPair,
            "google-kid",
            "https://accounts.google.com",
            "google-client-id",
            "google-sub",
            Map.of("email", "google@example.com", "name", "Google User", "picture", "https://example.com/me.png")
        );

        OAuthAttributes attrs = fixture.verifier.verify(idToken);

        assertThat(attrs.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(attrs.providerId()).isEqualTo("google-sub");
        assertThat(attrs.email()).isEqualTo("google@example.com");
        fixture.server.verify();
    }

    @Test
    @DisplayName("Google ID Token audience가 없으면 INVALID_OAUTH_TOKEN 예외를 던진다")
    void google_audience_missing() {
        TestFixture fixture = new TestFixture();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        fixture.expectGoogleJwks("google-kid", keyPair);
        String idToken = OidcTokenTestSupport.signedIdTokenWithoutAudience(
            keyPair,
            "google-kid",
            "https://accounts.google.com",
            "google-sub",
            Map.of("email", "google@example.com")
        );

        assertThatThrownBy(() -> fixture.verifier.verify(idToken))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
    }

    @Test
    @DisplayName("Google ID Token audience가 등록된 client id가 아니면 INVALID_OAUTH_TOKEN 예외를 던진다")
    void google_audience_mismatch() {
        TestFixture fixture = new TestFixture();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        fixture.expectGoogleJwks("google-kid", keyPair);
        String idToken = OidcTokenTestSupport.signedIdToken(
            keyPair,
            "google-kid",
            "https://accounts.google.com",
            "other-client-id",
            "google-sub",
            Map.of("email", "google@example.com")
        );

        assertThatThrownBy(() -> fixture.verifier.verify(idToken))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
    }

    private static class TestFixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final GoogleTokenVerifier verifier;

        TestFixture() {
            CacheUseCase cacheUseCase = new FakeCacheUseCase();
            OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
                builder.build(),
                new ObjectMapper(),
                cacheUseCase
            );
            verifier = new GoogleTokenVerifier(
                builder.build(),
                resolver,
                new GoogleOAuthProperties(
                    List.of("google-client-id"),
                    new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
                )
            );
        }

        void expectGoogleJwks(String kid, KeyPair keyPair) {
            server.expect(once(), requestTo("https://www.googleapis.com/oauth2/v3/certs"))
                .andRespond(withSuccess(
                    OidcTokenTestSupport.jwks(kid, (RSAPublicKey) keyPair.getPublic()),
                    MediaType.APPLICATION_JSON
                ));
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
