package com.ecommerce.secure.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

@Service
public class VaultEncryptionService {

    private final VaultTemplate vaultTemplate;
    private static final String KEY_NAME = "ecommerce-order-key";
    private static final String LOCAL_KEY = "LocalSecureECommercePlatformKey_"; // 32 bytes key for AES-256

    @Autowired
    public VaultEncryptionService(@Autowired(required = false) VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    // Mã hóa dữ liệu (Envelope Encryption) thông qua Vault Transit Engine hoặc Local AES-GCM
    public String encryptData(String plainText) {
        if (vaultTemplate != null) {
            try {
                Ciphertext ciphertext = vaultTemplate.opsForTransit().encrypt(KEY_NAME, Plaintext.of(plainText));
                return ciphertext.getCiphertext();
            } catch (Exception e) {
                // Fallback to local encryption if Vault call fails
            }
        }
        return encryptLocal(plainText);
    }

    // Giải mã dữ liệu
    public String decryptData(String cipherText) {
        if (vaultTemplate != null && cipherText != null && cipherText.startsWith("vault:")) {
            try {
                Plaintext plaintext = vaultTemplate.opsForTransit().decrypt(KEY_NAME, Ciphertext.of(cipherText));
                return plaintext.asString();
            } catch (Exception e) {
                // Fallback to local decryption if Vault call fails
            }
        }
        return decryptLocal(cipherText);
    }

    private String encryptLocal(String plainText) {
        try {
            byte[] keyBytes = LOCAL_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            byte[] iv = new byte[12];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(iv);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return java.util.Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Local AES encryption failed", e);
        }
    }

    private String decryptLocal(String cipherText) {
        try {
            byte[] combined = java.util.Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            byte[] keyBytes = LOCAL_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Local AES decryption failed", e);
        }
    }
}
