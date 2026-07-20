package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

@DisplayName("GoogleTokenVerifier 엣지 케이스")
class GoogleTokenVerifierEdgeCaseTest {

    @Nested
    @DisplayName("ID token 검증")
    class IdTokenVerification {

        @Test
        @DisplayName("Google이 허용하는 scheme 없는 issuer도 정상 검증한다")
        void scheme_없는_issuer도_허용한다() {
            Fixture fixture = new Fixture();
            KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
            fixture.expectJwks("google-kid", keyPair);
            String token = OidcTokenTestSupport.signedIdToken(
                keyPair,
                "google-kid",
                "accounts.google.com",
                "android-client-id",
                "google-user",
                Map.of()
            );

            OAuthAttributes result = fixture.verifier.verifyIdToken(token);

            assertThat(result.providerId()).isEqualTo("google-user");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("Google 이외 issuer는 INVALID_OAUTH_TOKEN으로 거부한다")
        void issuer_불일치를_거부한다() {
            Fixture fixture = new Fixture();
            KeyPair keyPair = OidcTokenTestSupport.rsaKeyPair();
            fixture.expectJwks("google-kid", keyPair);
            String token = OidcTokenTestSupport.signedIdToken(
                keyPair,
                "google-kid",
                "https://attacker.example",
                "google-client-id",
                "google-user",
                Map.of()
            );

            assertError(
                () -> fixture.verifier.verifyIdToken(token),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }

        @Test
        @DisplayName("JWT header가 손상되면 유효하지 않은 OAuth token 예외를 보존한다")
        void 손상된_jwt_header를_거부한다() {
            Fixture fixture = new Fixture();

            assertError(
                () -> fixture.verifier.verify("header.payload.signature"),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("Access token 검증")
    class AccessTokenVerification {

        @Test
        @DisplayName("opaque token은 tokeninfo endpoint에서 등록된 audience를 확인한다")
        void opaque_token을_tokeninfo로_검증한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo(
                "https://oauth2.googleapis.com/tokeninfo?access_token=opaque-token"
            )).andRespond(withSuccess("""
                {
                  "sub": "google-user",
                  "email": "google@example.com",
                  "aud": "google-client-id"
                }
                """, MediaType.APPLICATION_JSON));

            OAuthAttributes result = fixture.verifier.verify("opaque-token");

            assertThat(result.providerId()).isEqualTo("google-user");
            assertThat(result.email()).isEqualTo("google@example.com");
        }

        @Test
        @DisplayName("빈 token도 JWT로 오인하지 않고 tokeninfo 검증 경로를 사용한다")
        void 빈_token도_access_token_경로를_사용한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo(
                "https://oauth2.googleapis.com/tokeninfo?access_token="
            )).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

            assertError(
                () -> fixture.verifier.verify(""),
                AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN
            );
        }

        @Test
        @DisplayName("tokeninfo 응답이 없으면 검증 실패로 처리한다")
        void null_tokeninfo를_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo(
                "https://oauth2.googleapis.com/tokeninfo?access_token=token"
            )).andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertError(
                () -> fixture.verifier.verify("token"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("tokeninfo audience가 등록된 client ID가 아니면 거부한다")
        void tokeninfo_audience_불일치를_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo(
                "https://oauth2.googleapis.com/tokeninfo?access_token=token"
            )).andRespond(withSuccess("""
                {"sub":"google-user","email":"google@example.com","aud":"attacker-client"}
                """, MediaType.APPLICATION_JSON));

            assertError(
                () -> fixture.verifier.verify("token"),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }

        @Test
        @DisplayName("tokeninfo HTTP 오류는 invalid access token으로 구분한다")
        void tokeninfo_http_오류를_구분한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo(
                "https://oauth2.googleapis.com/tokeninfo?access_token=expired"
            )).andRespond(withStatus(HttpStatus.BAD_REQUEST));

            assertError(
                () -> fixture.verifier.verify("expired"),
                AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("Token revoke")
    class TokenRevoke {

        @Test
        @DisplayName("revoke token을 form body로 전달한다")
        void token을_form으로_전달한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://oauth2.googleapis.com/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("token=refresh-token")))
                .andRespond(withSuccess());

            fixture.verifier.revokeToken("refresh-token");

            fixture.server.verify();
        }

        @Test
        @DisplayName("revoke HTTP 오류를 token 검증 실패로 정규화한다")
        void revoke_http_오류를_변환한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://oauth2.googleapis.com/revoke"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertError(
                () -> fixture.verifier.revokeToken("refresh-token"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }
    }

    private static void assertError(Runnable action, AuthenticationErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(AuthenticationDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(errorCode)
            );
    }

    private static class Fixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final GoogleTokenVerifier verifier;

        Fixture() {
            OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
                builder.build(),
                new ObjectMapper(),
                new InMemoryCacheUseCase()
            );
            verifier = new GoogleTokenVerifier(
                builder.build(),
                resolver,
                new GoogleOAuthProperties(
                    List.of("google-client-id", "android-client-id"),
                    new OidcJwksCacheProperties(Duration.ofHours(1), 10L)
                )
            );
        }

        void expectJwks(String kid, KeyPair keyPair) {
            server.expect(once(), requestTo("https://www.googleapis.com/oauth2/v3/certs"))
                .andRespond(withSuccess(
                    OidcTokenTestSupport.jwks(kid, (RSAPublicKey) keyPair.getPublic()),
                    MediaType.APPLICATION_JSON
                ));
        }
    }

    private static class InMemoryCacheUseCase implements CacheUseCase {

        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
            Object value = storage.get(spec.namespace().value() + ":" + key.value());
            return value == null
                ? new CacheLookup.Miss<>()
                : new CacheLookup.Hit<>(spec.valueType().cast(value));
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
