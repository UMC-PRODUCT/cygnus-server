package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@DisplayName("AppleTokenVerifier")
class AppleTokenVerifierTest {

    @Test
    @DisplayName("같은 Apple kid의 ID Token 검증은 JWKS를 한 번만 조회한다")
    void same_apple_kid_uses_cached_jwks() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        server.expect(once(), requestTo("https://appleid.apple.com/auth/keys"))
            .andRespond(withSuccess(
                OidcTokenTestSupport.jwks("apple-kid", (RSAPublicKey) keyPair.getPublic()),
                MediaType.APPLICATION_JSON
            ));
        AppleTokenVerifier verifier = newVerifier(builder);
        String firstToken = appleToken(keyPair, "apple-kid", "apple-sub-1");
        String secondToken = appleToken(keyPair, "apple-kid", "apple-sub-2");

        OAuthAttributes first = verifier.verifyIdToken(firstToken, "apple-client-id");
        OAuthAttributes second = verifier.verifyIdToken(secondToken, "apple-client-id");

        assertThat(first.provider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(first.providerId()).isEqualTo("apple-sub-1");
        assertThat(second.providerId()).isEqualTo("apple-sub-2");
        server.verify();
    }

    private AppleTokenVerifier newVerifier(RestClient.Builder builder) {
        CacheUseCase cacheUseCase = new FakeCacheUseCase();
        OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
            builder.build(),
            new ObjectMapper(),
            cacheUseCase
        );
        return new AppleTokenVerifier(
            new AppleOAuthProperties(
                "apple-client-id",
                "apple-client-id",
                "team-id",
                "key-id",
                "private-key",
                new OidcJwksCacheProperties(Duration.ofHours(1), 1L)
            ),
            builder.build(),
            new ObjectMapper(),
            resolver
        );
    }

    private String appleToken(KeyPair keyPair, String kid, String subject) {
        return OidcTokenTestSupport.signedIdToken(
            keyPair,
            kid,
            "https://appleid.apple.com",
            "apple-client-id",
            subject,
            Map.of("email", subject + "@example.com")
        );
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
