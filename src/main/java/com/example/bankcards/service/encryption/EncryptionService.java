package com.example.bankcards.service.encryption;

import com.example.bankcards.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    @Value("${encryption.key}")
    private String encryptionKey;

    public String encrypt(String data) {
        try {
            SecretKey secretKey = new SecretKeySpec(
                    encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");

            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] encrypted = new byte[combined.length - IV_LENGTH_BYTE];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(combined, IV_LENGTH_BYTE, encrypted, 0, encrypted.length);

            SecretKey secretKey = new SecretKeySpec(
                    encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return String.format("**** **** **** %s", lastFour);
    }

    public String maskCVV() {
        return "***";
    }
}