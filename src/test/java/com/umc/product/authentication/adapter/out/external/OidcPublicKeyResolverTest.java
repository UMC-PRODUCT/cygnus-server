package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.security.KeyPair;
import java.security.PublicKey;
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
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("OidcPublicKeyResolver")
class OidcPublicKeyResolverTest {

    private static final String JWKS_URI = "https://issuer.example.test/.well-known/jwks.json";

    @Test
    @DisplayName("같은 namespace와 kid는 JWKS를 한 번만 조회하고 캐시된 공개키를 재사용한다")
    void same_kid_uses_cached_public_key() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        String jwks = OidcTokenTestSupport.jwks("kid-1", (RSAPublicKey) keyPair.getPublic());
        server.expect(once(), requestTo(JWKS_URI))
            .andRespond(withSuccess(jwks, MediaType.APPLICATION_JSON));
        OidcPublicKeyResolver resolver = newResolver(builder);

        PublicKey first = resolver.getPublicKey(spec(), "kid-1");
        PublicKey second = resolver.getPublicKey(spec(), "kid-1");

        assertThat(first).isSameAs(second);
        server.verify();
    }

    @Test
    @DisplayName("캐시된 JWKS에 kid가 없으면 JWKS를 새로 조회해 새 공개키를 캐시한다")
    void unknown_kid_refreshes_jwks() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeyPair oldKeyPair = OidcTokenTestSupport.rsaKeyPair();
        KeyPair newKeyPair = OidcTokenTestSupport.rsaKeyPair();
        server.expect(once(), requestTo(JWKS_URI))
            .andRespond(withSuccess(
                OidcTokenTestSupport.jwks("old-kid", (RSAPublicKey) oldKeyPair.getPublic()),
                MediaType.APPLICATION_JSON
            ));
        server.expect(once(), requestTo(JWKS_URI))
            .andRespond(withSuccess(
                OidcTokenTestSupport.jwks("new-kid", (RSAPublicKey) newKeyPair.getPublic()),
                MediaType.APPLICATION_JSON
            ));
        OidcPublicKeyResolver resolver = newResolver(builder);

        resolver.getPublicKey(spec(), "old-kid");
        PublicKey refreshed = resolver.getPublicKey(spec(), "new-kid");

        assertThat(refreshed).isEqualTo(newKeyPair.getPublic());
        server.verify();
    }

    @Test
    @DisplayName("JWKS에 비 RSA 키나 손상된 RSA 키가 있어도 유효한 RSA 공개키는 캐시한다")
    void skips_unsupported_or_corrupted_keys() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        String jwks = """
            {
              "keys": [
                {
                  "kid": "ec-kid",
                  "kty": "EC",
                  "alg": "ES256"
                },
                {
                  "kid": "broken-rsa-kid",
                  "kty": "RSA",
                  "alg": "RS256",
                  "n": "not-base64",
                  "e": "AQAB"
                },
                %s
              ]
            }
            """.formatted(OidcTokenTestSupport.jwk("valid-kid", (RSAPublicKey) keyPair.getPublic()));
        server.expect(once(), requestTo(JWKS_URI))
            .andRespond(withSuccess(jwks, MediaType.APPLICATION_JSON));
        OidcPublicKeyResolver resolver = newResolver(builder);

        PublicKey publicKey = resolver.getPublicKey(spec(), "valid-kid");

        assertThat(publicKey).isEqualTo(keyPair.getPublic());
        server.verify();
    }

    @Test
    @DisplayName("JWKS refresh 후에도 kid가 없으면 INVALID_OAUTH_TOKEN 예외를 던진다")
    void missing_kid_throws_invalid_oauth_token() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
        server.expect(once(), requestTo(JWKS_URI))
            .andRespond(withSuccess(
                OidcTokenTestSupport.jwks("different-kid", (RSAPublicKey) keyPair.getPublic()),
                MediaType.APPLICATION_JSON
            ));
        OidcPublicKeyResolver resolver = newResolver(builder);

        assertThatThrownBy(() -> resolver.getPublicKey(spec(), "missing-kid"))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
    }

    private OidcPublicKeyResolver newResolver(RestClient.Builder builder) {
        return new OidcPublicKeyResolver(builder.build(), new ObjectMapper(), new FakeCacheUseCase());
    }

    private OidcJwksSpec spec() {
        return new OidcJwksSpec(
            CacheNamespace.GOOGLE_JWKS,
            JWKS_URI,
            Duration.ofHours(1),
            1L
        );
    }

    private static class FakeCacheUseCase implements CacheUseCase {

        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
            Object value = storage.get(format(spec.namespace(), key));
            if (value == null) {
                return new CacheLookup.Miss<>();
            }
            return new CacheLookup.Hit<>(spec.valueType().cast(value));
        }

        @Override
        public <T> void put(CacheSpec<T> spec, CacheKey key, T value) {
            storage.put(format(spec.namespace(), key), value);
        }

        @Override
        public void evict(CacheNamespace namespace, CacheKey key) {
            storage.remove(format(namespace, key));
        }

        private String format(CacheNamespace namespace, CacheKey key) {
            return namespace.value() + ":" + key.value();
        }
    }
}
