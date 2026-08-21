package com.umc.product.demoday.adapter.out;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.GenerateDemodayEntryCodePort;

@Component
public class SecureRandomDemodayEntryCodeGenerator implements GenerateDemodayEntryCodePort {

    // O, I, L, 0, 1을 제거하여 입력 시 혼동을 줄이기 위함
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PREFIX = "GUEST-";
    private static final int CODE_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(PREFIX.length() + CODE_LENGTH);
        code.append(PREFIX);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(ALPHABET.length());
            code.append(ALPHABET.charAt(index));
        }

        return code.toString();
    }
}
