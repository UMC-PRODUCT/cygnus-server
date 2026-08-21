package com.umc.product.demoday.adapter.out;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;

@Component
public class SHA256DemodayEntryCodeHasher implements HashDemodayEntryCodePort {

    @Override
    public String hash(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawCode.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "JVM에서 SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
