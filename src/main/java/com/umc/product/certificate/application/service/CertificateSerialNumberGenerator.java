package com.umc.product.certificate.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.umc.product.certificate.domain.CertificateType;

@Component
public class CertificateSerialNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMdd")
        .withZone(ZoneOffset.UTC);

    private final SecureRandom secureRandom;

    public CertificateSerialNumberGenerator() {
        this(new SecureRandom());
    }

    CertificateSerialNumberGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate(CertificateType type, Instant issuedAt) {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return "UMC-%s-%s-%s".formatted(
            type.serialCode(),
            DATE_FORMATTER.format(issuedAt),
            suffix
        );
    }
}
