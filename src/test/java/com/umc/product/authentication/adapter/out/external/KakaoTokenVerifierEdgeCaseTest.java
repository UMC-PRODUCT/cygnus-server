package com.umc.product.authentication.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

@DisplayName("KakaoTokenVerifier 엣지 케이스")
class KakaoTokenVerifierEdgeCaseTest {

    private static final String CALLBACK = "https://app.example.com/oauth/kakao/callback";

    @Nested
    @DisplayName("인가 코드 교환")
    class AuthorizationCodeExchange {

        @Test
        @DisplayName("화이트리스트에 없는 redirect URI는 외부 호출 없이 거부한다")
        void 허용되지_않은_redirect_uri를_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(never(), requestTo("https://kauth.kakao.com/oauth/token"));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("code", "https://attacker.example/callback"),
                AuthenticationErrorCode.INVALID_OAUTH_REDIRECT_URI
            );
            fixture.server.verify();
        }

        @Test
        @DisplayName("id_token이 없으면 access token userinfo 조회로 안전하게 fallback한다")
        void access_token으로_fallback한다() {
            Fixture fixture = new Fixture("client-secret", "");
            fixture.server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("grant_type=authorization_code"),
                    org.hamcrest.Matchers.containsString("client_secret=client-secret"),
                    org.hamcrest.Matchers.containsString("code=authorization-code")
                )))
                .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
            fixture.expectUserInfo("""
                {
                  "id": 123,
                  "kakao_account": {
                    "email": "kakao@example.com",
                    "profile": {"nickname": "카카오"}
                  }
                }
                """);

            OAuthAttributes result = fixture.verifier.verifyAuthorizationCode("authorization-code", CALLBACK);

            assertThat(result.providerId()).isEqualTo("123");
            assertThat(result.email()).isEqualTo("kakao@example.com");
            fixture.server.verify();
        }

        @Test
        @DisplayName("client secret이 비어 있으면 토큰 교환 form에 포함하지 않는다")
        void 빈_client_secret은_form에서_제외한다() {
            Fixture fixture = new Fixture("", "");
            fixture.server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("client_secret")
                )))
                .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
            fixture.expectUserInfo("{\"id\":123,\"kakao_account\":{\"email\":null}}");

            OAuthAttributes result = fixture.verifier.verifyAuthorizationCode("code", CALLBACK);

            assertThat(result.email()).isNull();
            fixture.server.verify();
        }

        @Test
        @DisplayName("토큰 교환 응답에 사용할 토큰이 없으면 검증 실패로 정규화한다")
        void 빈_토큰_응답을_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("code", CALLBACK),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("토큰 endpoint의 오류 상태는 유효하지 않은 OAuth token으로 전달한다")
        void 토큰_endpoint_오류를_전달한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

            assertError(
                () -> fixture.verifier.verifyAuthorizationCode("expired-code", CALLBACK),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("Access token 검증")
    class AccessTokenVerification {

        @Test
        @DisplayName("userinfo 응답이 없으면 provider 정보를 만들지 않는다")
        void null_userinfo를_거부한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertError(
                () -> fixture.verifier.verifyAccessToken("access-token"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("userinfo의 사용자 ID가 null이면 검증 실패로 처리한다")
        void null_사용자_id를_거부한다() {
            Fixture fixture = new Fixture();
            fixture.expectUserInfo("{\"id\":null,\"kakao_account\":{\"email\":\"a@b.com\"}}");

            assertError(
                () -> fixture.verifier.verifyAccessToken("access-token"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("profile이 없어도 사용자 ID와 이메일은 정상적으로 정규화한다")
        void profile_없는_응답을_허용한다() {
            Fixture fixture = new Fixture();
            fixture.expectUserInfo("{\"id\":123,\"kakao_account\":{\"email\":\"a@b.com\",\"profile\":null}}");

            OAuthAttributes result = fixture.verifier.verifyAccessToken("access-token");

            assertThat(result.providerId()).isEqualTo("123");
            assertThat(result.email()).isEqualTo("a@b.com");
        }

        @Test
        @DisplayName("userinfo HTTP 오류는 INVALID_OAUTH_TOKEN으로 구분한다")
        void userinfo_http_오류를_구분한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer bad-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

            assertError(
                () -> fixture.verifier.verifyAccessToken("bad-token"),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }

        @Test
        @DisplayName("점이 세 구간이 아닌 token은 access token 경로로 검증한다")
        void jwt_형식이_아니면_access_token으로_검증한다() {
            Fixture fixture = new Fixture();
            fixture.expectUserInfo("{\"id\":55,\"kakao_account\":{\"email\":\"a@b.com\"}}");

            OAuthAttributes result = fixture.verifier.verify("opaque-access-token");

            assertThat(result.providerId()).isEqualTo("55");
        }

        @Test
        @DisplayName("JWT header에서 kid를 읽을 수 없으면 유효하지 않은 token으로 구분한다")
        void 잘못된_jwt를_거부한다() {
            Fixture fixture = new Fixture();

            assertError(
                () -> fixture.verifier.verify("header.payload.signature"),
                AuthenticationErrorCode.INVALID_OAUTH_TOKEN
            );
        }
    }

    @Nested
    @DisplayName("연결 해제")
    class Unlink {

        @Test
        @DisplayName("사용자 access token을 Authorization header로 전달한다")
        void access_token으로_연결을_해제한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess());

            fixture.verifier.unlinkUser("access-token");

            fixture.server.verify();
        }

        @Test
        @DisplayName("사용자 연결 해제 HTTP 오류를 검증 실패로 변환한다")
        void 연결_해제_http_오류를_변환한다() {
            Fixture fixture = new Fixture();
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertError(
                () -> fixture.verifier.unlinkUser("access-token"),
                AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED
            );
        }

        @Test
        @DisplayName("Admin key가 없으면 provider ID 유무와 관계없이 외부 호출을 생략한다")
        void admin_key가_없으면_호출하지_않는다() {
            Fixture fixture = new Fixture("", "");
            fixture.server.expect(never(), requestTo("https://kapi.kakao.com/v1/user/unlink"));

            fixture.verifier.unlinkUserByAdmin("123");
            fixture.verifier.unlinkUserByAdmin(null);

            fixture.server.verify();
        }

        @Test
        @DisplayName("Admin 연결 해제는 KakaoAK header와 provider ID를 form으로 전달한다")
        void admin_key로_연결을_해제한다() {
            Fixture fixture = new Fixture("", "admin-key");
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andExpect(header("Authorization", "KakaoAK admin-key"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("target_id_type=user_id"),
                    org.hamcrest.Matchers.containsString("target_id=123")
                )))
                .andRespond(withSuccess());

            fixture.verifier.unlinkUserByAdmin("123");

            fixture.server.verify();
        }

        @Test
        @DisplayName("Admin 연결 해제 HTTP 오류를 검증 실패로 보존한다")
        void admin_연결_해제_http_오류를_변환한다() {
            Fixture fixture = new Fixture("", "admin-key");
            fixture.server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertError(
                () -> fixture.verifier.unlinkUserByAdmin("123"),
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
        private final KakaoTokenVerifier verifier;

        Fixture() {
            this("", "");
        }

        Fixture(String clientSecret, String adminKey) {
            OidcPublicKeyResolver resolver = new OidcPublicKeyResolver(
                builder.build(),
                new ObjectMapper(),
                new InMemoryCacheUseCase()
            );
            verifier = new KakaoTokenVerifier(
                builder.build(),
                new KakaoOAuthProperties(
                    "kakao-rest-api-key",
                    clientSecret,
                    adminKey,
                    List.of(CALLBACK),
                    new OidcJwksCacheProperties(Duration.ofHours(1), 10L)
                ),
                resolver
            );
        }

        void expectUserInfo(String body) {
            server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
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
