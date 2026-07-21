package com.umc.product.recruiting.adapter.out.id;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.GenerateRecruitingApplicationKeyPort;

@Component
public class RecruitingApplicationKeyGenerator implements GenerateRecruitingApplicationKeyPort {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int KEY_LENGTH = 6;

    private final SecureRandom secureRandom;

    public RecruitingApplicationKeyGenerator() {
        this(new SecureRandom());
    }

    RecruitingApplicationKeyGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public String generate() {
        char[] key = new char[KEY_LENGTH];
        for (int index = 0; index < KEY_LENGTH; index++) {
            key[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(key);
    }
}
