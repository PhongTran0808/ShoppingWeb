package com.ecommerce.secure.order.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class AesCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static String encryptionKey;

    // Inject encryption key from config (Vault or fallback)
    @Value("${encryption.aes.key:0123456789abcdef0123456789abcdef}")
    public void setEncryptionKey(String key) {
        AesCryptoConverter.encryptionKey = key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            // Generate Random IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Prefix IV to CipherText for later decryption
            byte[] ivAndCipherText = new byte[IV_LENGTH_BYTE + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipherText, 0, IV_LENGTH_BYTE);
            System.arraycopy(cipherText, 0, ivAndCipherText, IV_LENGTH_BYTE, cipherText.length);

            return Base64.getEncoder().encodeToString(ivAndCipherText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data (AES-GCM)", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] ivAndCipherText = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[ivAndCipherText.length - IV_LENGTH_BYTE];

            // Extract IV and CipherText
            System.arraycopy(ivAndCipherText, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(ivAndCipherText, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data (AES-GCM)", e);
        }
    }
}
