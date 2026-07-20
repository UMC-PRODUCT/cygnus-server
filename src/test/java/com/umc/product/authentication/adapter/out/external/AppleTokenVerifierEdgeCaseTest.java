package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
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
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@DisplayName("AppleTokenVerifier 엣지 케이스")
class AppleTokenVerifierEdgeCaseTest {

    @Nested
    @DisplayName("ID token 검증")
    class IdTokenVerification {

        @Test
        @DisplayName("email claim이 없어도 provider ID를 보존한다")
        void email이_없어도_provider_id를_보존한다() {
            Fixture fixture = new Fixture();
            KeyPair signingKey = OidcTokenTestSupport.rsaKeyPair();
            fixture.expectJwks("apple-kid", signingKey);
            String token = fixture.appleIdToken(signingKey, "apple-kid", "apple-ios-client", Map.of());

            OAuthAttributes result = fixture.verifier.verifyIdToken(token, "apple-ios-client");

            assertThat(result.providerId()).isEqualTo("apple-user");
            assertThat(result.email()).isNull();
        }

        @Test
        @DisplayName("issuer가 Apple이 아니면 공통 token 검증 실패로 변환한다")
        void issuer_불일치를_거부한다() {
            Fixture fixture = new Fixture();
            KeyPair signingKey = OidcTokenTestSupport.rsaKeyPair();
            fixture.expectJwks("apple-kid", signingKey);
            String token = OidcTokenTestSupport.signedIdToken(
                signingKey,
                "apple-kid",
                "https://attacker.example",
                "apple-ios-client",
                "apple-user",
                Map.of()
            );

            assertError(
                () -> fixture.verifier.verifyIdToken(token, "apple-ios-client"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("audience가 요청 client ID와 다르면 검증 실패로 처리한다")
        void audience_불일치를_거부한다() {
            Fixture fixture = new Fixture();
            KeyPair signingKey = OidcTokenTestSupport.rsaKeyPair();
            fixture.expectJwks("apple-kid", signingKey);
            String token = fixture.appleIdToken(signingKey, "apple-kid", "another-client", Map.of());

            assertError(
                () -> fixture.verifier.verifyIdToken(token, "apple-ios-client"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("JWT header에 kid가 없으면 유효하지 않은 OAuth token 예외를 보존한다")
        void kid_없는_token을_거부한다() {
            Fixture fixture = new Fixture();

            assertError(
                () -> fixture.verifier.verifyIdToken("header.payload.signature", "apple-ios-client"),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("인가 코드 교환")
    class AuthorizationCodeExchange {

        @Test
        @DisplayName("iOS 인가 코드는 iOS client ID로 교환하고 refresh token을 함께 반환한다")
        void ios_client_id로_교환한다() {
            Fixture fixture = new Fixture();
            KeyPair signingKey = OidcTokenTestSupport.rsaKeyPair();
            String idToken = fixture.appleIdToken(
                signingKey,
                "apple-kid",
                "apple-ios-client",
                Map.of("email", "apple@example.com")
            );
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("client_id=apple-ios-client"),
                    org.hamcrest.Matchers.containsString("code=authorization-code"),
                    org.hamcrest.Matchers.containsString("grant_type=authorization_code")
                )))
                .andRespond(withSuccess("""
                    {
                      "refresh_token": "apple-refresh-token",
                      "id_token": "%s"
                    }
                    """.formatted(idToken), MediaType.APPLICATION_JSON));
            fixture.expectJwks("apple-kid", signingKey);

            AppleAuthorizationCodeResult result =
                fixture.verifier.verifyAuthorizationCode("authorization-code", ClientType.IOS);

            assertThat(result.attrs().providerId()).isEqualTo("apple-user");
            assertThat(result.refreshToken()).isEqualTo("apple-refresh-token");
            assertThat(result.clientId()).isEqualTo("apple-ios-client");
            fixture.server.verify();
        }

        @Test
        @DisplayName("WEB과 ANDROID는 web client ID를 사용한다")
        void web과_android는_web_client_id를_사용한다() {
            AppleOAuthProperties properties = Fixture.properties(Fixture.privateKeyPem(Fixture.ecKeyPair()));

            assertThat(properties.resolveClientId(ClientType.WEB)).isEqualTo("apple-web-client");
            assertThat(properties.resolveClientId(ClientType.ANDROID)).isEqualTo("apple-web-client");
            assertThat(properties.resolveClientId(ClientType.IOS)).isEqualTo("apple-ios-client");
        }

        @Test
        @DisplayName("token endpoint 응답에 id_token이 없으면 검증 실패로 처리한다")
        void id_token_없는_응답을_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/token"))
                .andRespond(withSuccess("{\"refresh_token\":\"refresh\"}", MediaType.APPLICATION_JSON));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("authorization-code", ClientType.IOS),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("Apple token endpoint 오류는 invalid access token으로 구분한다")
        void token_endpoint_오류를_구분한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"invalid_grant\"}"));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("expired-code", ClientType.IOS),
                AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN
            );
        }

        @Test
        @DisplayName("Apple 오류 body가 JSON이 아니어도 token 원문을 노출하지 않고 동일 예외로 처리한다")
        void 손상된_오류_body도_안전하게_처리한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("not-json-sensitive-body"));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("expired-code", ClientType.IOS),
                AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("client secret")
    class ClientSecret {

        @Test
        @DisplayName("ES256 client secret에 Apple 필수 issuer·audience·subject를 담는다")
        void 필수_claim을_담는다() {
            KeyPair clientSecretKey = Fixture.ecKeyPair();
            Fixture fixture = new Fixture(clientSecretKey);

            String secret = fixture.verifier.generateClientSecret("apple-ios-client");
            Claims claims = Jwts.parser()
                .verifyWith((ECPublicKey) clientSecretKey.getPublic())
                .build()
                .parseSignedClaims(secret)
                .getPayload();

            assertThat(claims.getIssuer()).isEqualTo("apple-team");
            assertThat(claims.getAudience()).contains("https://appleid.apple.com");
            assertThat(claims.getSubject()).isEqualTo("apple-ios-client");
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("private key는 한 번 파싱한 뒤 캐시하여 재사용한다")
        void private_key를_캐시한다() {
            Fixture fixture = new Fixture();

            String first = fixture.verifier.generateClientSecret("first-client");
            String second = fixture.verifier.generateClientSecret("second-client");

            assertThat(first).isNotBlank();
            assertThat(second).isNotBlank().isNotEqualTo(first);
        }

        @Test
        @DisplayName("손상된 private key는 인증 도메인 예외로 정규화한다")
        void 손상된_private_key를_거부한다() {
            Fixture fixture = new Fixture("not-a-pem-key");

            assertError(
                () -> fixture.verifier.generateClientSecret("apple-ios-client"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }
    }

    @Nested
    @DisplayName("refresh token 폐기")
    class TokenRevoke {

        @Test
        @DisplayName("저장 당시 client ID와 refresh token을 Apple revoke endpoint에 전달한다")
        void 저장된_client_id로_revoke한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("client_id=stored-client"),
                    org.hamcrest.Matchers.containsString("token=refresh-token"),
                    org.hamcrest.Matchers.containsString("token_type_hint=refresh_token")
                )))
                .andRespond(withSuccess());

            fixture.verifier.revokeToken("refresh-token", "stored-client");

            fixture.server.verify();
        }

        @Test
        @DisplayName("revoke endpoint 오류를 token 검증 실패로 변환한다")
        void revoke_오류를_변환한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://appleid.apple.com/auth/revoke"))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString("application/x-www-form-urlencoded")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(""));

            assertError(
                () -> fixture.verifier.revokeToken("refresh-token", "stored-client"),
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
        private final AppleTokenVerifier verifier;

        Fixture() {
            this(ecKeyPair());
        }

        Fixture(KeyPair clientSecretKey) {
            this(privateKeyPem(clientSecretKey));
        }

        Fixture(String privateKey) {
            ObjectMapper objectMapper = new ObjectMapper();
            OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
                builder.build(),
                objectMapper,
                new InMemoryCacheUseCase()
            );
            verifier = new AppleTokenVerifier(
                properties(privateKey),
                builder.build(),
                objectMapper,
                resolver
            );
        }

        void expectJwks(String kid, KeyPair signingKey) {
            server.expect(once(), requestTo("https://appleid.apple.com/auth/keys"))
                .andRespond(withSuccess(
                    OidcTokenTestSupport.jwks(kid, (RSAPublicKey) signingKey.getPublic()),
                    MediaType.APPLICATION_JSON
                ));
        }

        String appleIdToken(KeyPair signingKey, String kid, String audience, Map<String, Object> claims) {
            return OidcTokenTestSupport.signedIdToken(
                signingKey,
                kid,
                "https://appleid.apple.com",
                audience,
                "apple-user",
                claims
            );
        }

        static AppleOAuthProperties properties(String privateKey) {
            return new AppleOAuthProperties(
                "apple-ios-client",
                "apple-web-client",
                "apple-team",
                "apple-key-id",
                privateKey,
                new OidcJwksCacheProperties(Duration.ofHours(1), 10L)
            );
        }

        static KeyPair ecKeyPair() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(256);
                return generator.generateKeyPair();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        static String privateKeyPem(KeyPair keyPair) {
            String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
            return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
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
