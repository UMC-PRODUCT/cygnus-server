package com.umc.product.authentication.adapter.out.external;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OidcPublicKeyResolver {

    private static final CacheKey PUBLIC_KEYS_CACHE_KEY = CacheKey.from("public-keys");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CacheUseCase cacheUseCase;

    public String extractKid(String idToken) {
        try {
            String header = idToken.split("\\.")[0];
            byte[] decoded = Base64.getUrlDecoder().decode(header);
            JsonNode kid = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8)).path("kid");
            if (!kid.isTextual() || kid.asText().isBlank()) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
            }
            return kid.asText();
        } catch (AuthenticationDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public PublicKey getPublicKey(OidcJwksSpec spec, String kid) {
        CacheSpec<OidcPublicKeys> cacheSpec = cacheSpec(spec);
        PublicKey cached = findCachedPublicKey(cacheSpec, kid);
        if (cached != null) {
            return cached;
        }

        synchronized (spec.namespace()) {
            cached = findCachedPublicKey(cacheSpec, kid);
            if (cached != null) {
                return cached;
            }

            OidcPublicKeys refreshed = fetchPublicKeys(spec);
            cacheUseCase.put(cacheSpec, PUBLIC_KEYS_CACHE_KEY, refreshed);
            PublicKey publicKey = refreshed.get(kid);
            if (publicKey == null) {
                log.error("OIDC JWKS에서 kid={}에 매칭되는 키를 찾을 수 없음: namespace={}", kid, spec.namespace());
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_OAUTH_TOKEN);
            }
            return publicKey;
        }
    }

    private PublicKey findCachedPublicKey(CacheSpec<OidcPublicKeys> cacheSpec, String kid) {
        CacheLookup<OidcPublicKeys> lookup = cacheUseCase.get(cacheSpec, PUBLIC_KEYS_CACHE_KEY);
        if (lookup instanceof CacheLookup.Hit<OidcPublicKeys> hit) {
            return hit.value().get(kid);
        }
        return null;
    }

    private OidcPublicKeys fetchPublicKeys(OidcJwksSpec spec) {
        OidcJwksResponse jwks = restClient.get()
            .uri(spec.jwksUri())
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                log.error("OIDC JWKS 조회 실패: namespace={}, status={}", spec.namespace(), res.getStatusCode());
                throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            })
            .body(OidcJwksResponse.class);

        if (jwks == null || jwks.keys() == null || jwks.keys().isEmpty()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }

        Map<String, PublicKey> keys = jwks.keys().stream()
            .filter(key -> key.kid() != null && !key.kid().isBlank())
            .filter(key -> "RSA".equalsIgnoreCase(key.kty()))
            .map(this::buildPublicKeyEntry)
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        return new OidcPublicKeys(keys);
    }

    private Optional<Map.Entry<String, PublicKey>> buildPublicKeyEntry(OidcJwk jwk) {
        try {
            return Optional.of(Map.entry(jwk.kid(), buildRsaPublicKey(jwk)));
        } catch (Exception e) {
            log.warn("OIDC RSA 공개키 생성 실패, 해당 키를 건너뜁니다: kid={}", jwk.kid(), e);
            return Optional.empty();
        }
    }

    private PublicKey buildRsaPublicKey(OidcJwk jwk) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(jwk.n());
        byte[] eBytes = Base64.getUrlDecoder().decode(jwk.e());

        RSAPublicKeySpec spec = new RSAPublicKeySpec(
            new BigInteger(1, nBytes),
            new BigInteger(1, eBytes)
        );
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private CacheSpec<OidcPublicKeys> cacheSpec(OidcJwksSpec spec) {
        return CacheSpec.of(spec.namespace(), OidcPublicKeys.class, spec.ttl(), spec.maxSize());
    }

    private record OidcJwksResponse(List<OidcJwk> keys) {
    }

    private record OidcJwk(
        String kty,
        String kid,
        String use,
        String alg,
        String n,
        String e
    ) {
    }

    private record OidcPublicKeys(Map<String, PublicKey> keys) {

        PublicKey get(String kid) {
            return keys.get(kid);
        }
    }
}
