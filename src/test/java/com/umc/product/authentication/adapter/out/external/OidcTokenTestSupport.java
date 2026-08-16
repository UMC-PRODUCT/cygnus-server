package com.umc.product.authentication.adapter.out.external;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;

final class OidcTokenTestSupport {

    private OidcTokenTestSupport() {
    }

    static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String signedIdToken(
        KeyPair keyPair,
        String kid,
        String issuer,
        String audience,
        String subject,
        Map<String, Object> extraClaims
    ) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .header()
            .add("kid", kid)
            .and()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)));

        extraClaims.forEach(builder::claim);

        return builder.signWith(keyPair.getPrivate(), SIG.RS256).compact();
    }

    static String jwks(String kid, RSAPublicKey publicKey) {
        return """
            {
              "keys": [
                %s
              ]
            }
            """.formatted(jwk(kid, publicKey));
    }

    static String jwk(String kid, RSAPublicKey publicKey) {
        return """
            {
              "kid": "%s",
              "kty": "RSA",
              "alg": "RS256",
              "use": "sig",
              "n": "%s",
              "e": "%s"
            }
            """.formatted(
            kid,
            base64UrlUnsigned(publicKey.getModulus()),
            base64UrlUnsigned(publicKey.getPublicExponent())
        );
    }

    static String signedIdTokenWithoutAudience(
        KeyPair keyPair,
        String kid,
        String issuer,
        String subject,
        Map<String, Object> extraClaims
    ) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .header()
            .add("kid", kid)
            .and()
            .issuer(issuer)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)));

        extraClaims.forEach(builder::claim);

        return builder.signWith(keyPair.getPrivate(), SIG.RS256).compact();
    }

    private static String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
