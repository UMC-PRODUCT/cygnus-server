package com.umc.product.demoday.adapter.out;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.EncryptDemodayStampCredentialPort;
import com.umc.product.demoday.config.DemodayStampCredentialProperties;

@Component
public class AesGcmDemodayStampCredentialEncryptor implements EncryptDemodayStampCredentialPort {

    private static final int AES_256_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public AesGcmDemodayStampCredentialEncryptor(
        DemodayStampCredentialProperties properties
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.encryptionKey());

        if (keyBytes.length != AES_256_KEY_LENGTH) {
            throw new IllegalStateException("Demoday 스탬프 credential 암호화 키는 32바이트여야 합니다.");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            //Java GCM의 결과값에는 tag가 뒤에 붙음
            byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] encrypted = ByteBuffer.allocate(iv.length + cipherTextWithTag.length)
                .put(iv)
                .put(cipherTextWithTag)
                .array();

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("스탬프 credential 암호화에 실패했습니다.", exception);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        try {
            byte[] encrypted = Base64.getUrlDecoder().decode(encryptedText);

            if (encrypted.length <= IV_LENGTH) {
                throw new IllegalArgumentException("암호문 형식이 올바르지 않습니다.");
            }

            byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_LENGTH);
            byte[] cipherTextWithTag = Arrays.copyOfRange(encrypted, IV_LENGTH, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            byte[] plainText = cipher.doFinal(cipherTextWithTag);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("스탬프 credential 복호화에 실패했습니다.", exception);
        }
    }
}
