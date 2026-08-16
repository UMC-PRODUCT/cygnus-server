package com.umc.product.authentication.adapter.out.external;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.logging.ExternalApiCallLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Apple Sign-In 토큰 검증 Adapter
 * <p>
 * Apple ID Token(JWT) 서명 검증과 Authorization Code 교환을 담당합니다.
 * <p>
 * Apple Sign-In은 플랫폼별로 다른 client_id를 사용하므로(iOS Bundle ID vs Web Services ID),
 * authorization code 교환 / token revoke 시 클라이언트 플랫폼에 맞는 client_id를 사용해야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AppleOAuthProperties appleProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OidcPublicKeyResolver publicKeyResolver;
    private PrivateKey cachedPrivateKey;

    /**
     * Apple ID Token(JWT)을 검증하고 OAuthAttributes로 변환합니다.
     * <p>
     * Apple JWKS 공개키로 서명을 검증하고, issuer/audience를 확인합니다.
     *
     * @param idToken  Apple에서 발급받은 identity token (JWT)
     * @param clientId audience 검증에 사용할 client_id (플랫폼별로 다름)
     * @return OAuthAttributes
     * @throws AuthenticationDomainException 토큰 검증 실패 시
     */
    public OAuthAttributes verifyIdToken(String idToken, String clientId) {
        log.debug("Apple ID Token 검증 시작: clientId={}", clientId);

        try {
            String kid = publicKeyResolver.extractKid(idToken);
            PublicKey publicKey = publicKeyResolver.getPublicKey(appleJwksSpec(), kid);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(APPLE_ISSUER)
                .requireAudience(clientId)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

            String sub = claims.getSubject();
            String email = claims.get("email", String.class);

            log.debug("Apple ID Token을 검증했습니다: hasEmail={}", hasEmail(email));

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", sub);
            attributes.put("email", email);

            return OAuthAttributes.of("apple", attributes);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple ID Token 검증 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Apple Authorization Code를 교환하여 사용자 정보를 가져옵니다.
     * <p>
     * Authorization Code로 Apple token endpoint에 요청하여 id_token을 받은 뒤, 해당 토큰을 검증합니다.
     *
     * @param authorizationCode Apple에서 발급받은 authorization code
     * @param clientType        클라이언트 플랫폼 (Apple client_id 매칭용)
     * @return OAuthAttributes, refresh token, 그리고 사용된 client_id (DB에 저장하기 위함)
     * @throws AuthenticationDomainException 코드 교환 또는 토큰 검증 실패 시
     */
    public AppleAuthorizationCodeResult verifyAuthorizationCode(String authorizationCode, ClientType clientType) {
        log.debug("Apple Authorization Code 교환 시작: clientType={}", clientType);

        try {
            String clientId = appleProperties.resolveClientId(clientType);
            String clientSecret = generateClientSecret(clientId);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("code", authorizationCode);
            formData.add("grant_type", "authorization_code");

            AppleTokenResponse response = ExternalApiCallLogger.measure("APPLE", "EXCHANGE_AUTHORIZATION_CODE", () ->
                restClient.post()
                    .uri(APPLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        // ADR-016 §민감 필드 정책: 성공 응답 body 에는 access_token / id_token / refresh_token
                        // 이 포함되어 있어 통째로 로깅하면 토큰이 stdout / Loki 에 남는다. 에러 응답이라도
                        // 잘못된 분기로 성공 body 가 흘러올 수 있으므로 status / errorCode 만 남긴다.
                        String body = StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8);
                        String errorCode = extractAppleErrorCode(body);
                        log.warn("Apple token endpoint 호출 실패: status={}, errorCode={}, bodyLength={}",
                            res.getStatusCode(), errorCode, body.length());
                        throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN);
                    })
                    .body(AppleTokenResponse.class)
            );

            if (response == null || response.idToken() == null) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }

            log.info("Apple Authorization Code를 교환했습니다. ID Token을 검증합니다");

            OAuthAttributes attrs = verifyIdToken(response.idToken(), clientId);
            return new AppleAuthorizationCodeResult(attrs, response.refreshToken(), clientId);

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple Authorization Code 교환 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Apple refresh token을 revoke합니다.
     *
     * @param refreshToken Apple에서 발급받은 refresh token
     * @param clientId     해당 refresh token을 발급받을 때 사용한 client_id (DB에 저장된 값)
     */
    public void revokeToken(String refreshToken, String clientId) {
        log.info("Apple token revoke 시작: clientId={}", clientId);

        try {
            String clientSecret = generateClientSecret(clientId);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("token", refreshToken);
            formData.add("token_type_hint", "refresh_token");

            ExternalApiCallLogger.measure("APPLE", "REVOKE_TOKEN", () ->
                restClient.post()
                    .uri(APPLE_REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        // ADR-016 §민감 필드 정책: revoke 응답 본문 통째로 로깅하지 않는다.
                        String body = StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8);
                        String errorCode = extractAppleErrorCode(body);
                        log.warn("Apple token revoke 실패: status={}, errorCode={}, bodyLength={}",
                            res.getStatusCode(), errorCode, body.length());
                        throw new AuthenticationDomainException(
                            AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
                    })
                    .toBodilessEntity()
            );

            log.info("Apple token revoke를 완료했습니다");

        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple token revoke 실패", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Apple client_secret JWT를 생성합니다.
     * <p>
     * Apple Developer 문서에 따라 ES256으로 서명된 JWT를 생성하며,
     * subject claim에는 호출 시 전달된 client_id가 들어갑니다.
     *
     * @param clientId client_secret JWT의 subject claim으로 사용할 client_id
     * @see <a
     * href="https://developer.apple.com/documentation/accountorganizationaldatasharing/creating-a-client-secret">Creating
     * a client secret</a>
     */
    public String generateClientSecret(String clientId) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(3600);

            return Jwts.builder()
                .header()
                .add("kid", appleProperties.keyId())
                .and()
                .issuer(appleProperties.teamId())
                .issuedAt(Date.from(now)).expiration(Date.from(expiration))
                .audience().add(APPLE_ISSUER).and()
                .subject(clientId)
                .signWith(getPrivateKey(), SIG.ES256)
                .compact();

        } catch (Exception e) {
            log.error("Failed to generate Apple client secret", e);
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Apple token endpoint 의 에러 응답 JSON 에서 {@code error} 코드만 안전하게 꺼낸다.
     *
     * <p>응답 본문 전체를 로그에 남기면 잘못 분기되어 성공 응답이 흘러왔을 때 access_token /
     * id_token / refresh_token 이 stdout 으로 새어나갈 수 있다. 따라서 Jackson 으로 트리를
     * 파싱한 뒤 {@code error} 필드 값만 추출하고, 파싱 실패 또는 필드 부재 시 {@code "unknown"}
     * 으로 대체한다 (수동 indexOf 파싱은 공백·필드 순서·유사 키 변화에 취약하므로 사용하지 않는다).
     */
    private String extractAppleErrorCode(String body) {
        if (body == null || body.isEmpty()) {
            return "unknown";
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            return error.isTextual() ? error.asText() : "unknown";
        } catch (JsonProcessingException e) {
            return "unknown";
        }
    }

    private PrivateKey getPrivateKey() throws Exception {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }

        // 환경변수에서 \n 리터럴이 들어올 수 있으므로 실제 줄바꿈으로 변환
        String keyStr = appleProperties.privateKey().replace("\\n", "\n");
        log.debug("Private key length: {}", keyStr.length());

        try (StringReader keyReader = new StringReader(keyStr);
             PEMParser pemParser = new PEMParser(keyReader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
            cachedPrivateKey = converter.getPrivateKey(privateKeyInfo);

            return cachedPrivateKey;
        }
    }

    private OidcJwksSpec appleJwksSpec() {
        return new OidcJwksSpec(
            CacheNamespace.APPLE_JWKS,
            APPLE_JWKS_URL,
            appleProperties.jwksCache().ttl(),
            appleProperties.jwksCache().maxSize()
        );
    }

    private boolean hasEmail(String email) {
        return email != null && !email.isBlank();
    }

    // ===== Response DTOs =====

    private record AppleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken
    ) {
    }
}
