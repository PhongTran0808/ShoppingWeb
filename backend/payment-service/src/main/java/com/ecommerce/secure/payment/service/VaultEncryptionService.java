package com.ecommerce.secure.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

@Service
public class VaultEncryptionService {

    private final VaultTemplate vaultTemplate;
    private static final String KEY_NAME = "ecommerce-order-key";

    public VaultEncryptionService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    // Mã hóa dữ liệu (Envelope Encryption) thông qua Vault Transit Engine
    public String encryptData(String plainText) {
        Ciphertext ciphertext = vaultTemplate.opsForTransit().encrypt(KEY_NAME, Plaintext.of(plainText));
        return ciphertext.getCiphertext();
    }

    // Giải mã dữ liệu
    public String decryptData(String cipherText) {
        Plaintext plaintext = vaultTemplate.opsForTransit().decrypt(KEY_NAME, Ciphertext.of(cipherText));
        return plaintext.asString();
    }
}
