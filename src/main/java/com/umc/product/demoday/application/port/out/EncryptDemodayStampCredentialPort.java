package com.umc.product.demoday.application.port.out;

public interface EncryptDemodayStampCredentialPort {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
