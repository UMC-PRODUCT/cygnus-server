package com.umc.product.demoday.adapter.out;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.GenerateDemodayStampCredentialPort;

@Component
public class SecureRandomDemodayStampCredentialGenerator implements GenerateDemodayStampCredentialPort {

    private static final int CREDENTIAL_BYTE_LENGTH = 44;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[CREDENTIAL_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }
}
