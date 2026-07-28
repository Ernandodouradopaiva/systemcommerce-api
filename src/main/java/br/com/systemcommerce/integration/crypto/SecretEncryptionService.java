package br.com.systemcommerce.integration.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cifra credenciais de integração com AES-GCM. A chave vem de configuração — nunca do frontend.
 */
@Service
public class SecretEncryptionService {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretEncryptionService(
            @Value("${systemcommerce.integration.secrets-key:}") String configuredKey,
            @Value("${app.security.jwt.secret:change-me-systemcommerce-jwt-secret-key-min-256-bits-long}")
                    String jwtFallback) {
        byte[] raw = decodeKey(configuredKey != null && !configuredKey.isBlank() ? configuredKey : jwtFallback);
        if (raw.length < 16) {
            throw new IllegalStateException(
                    "Chave de secrets de integração inválida (mín. 16 bytes). Defina systemcommerce.integration.secrets-key.");
        }
        byte[] aesKey = new byte[32];
        System.arraycopy(raw, 0, aesKey, 0, Math.min(raw.length, 32));
        if (raw.length < 32) {
            for (int i = raw.length; i < 32; i++) {
                aesKey[i] = (byte) (raw[i % raw.length] ^ (i * 31));
            }
        }
        this.key = new SecretKeySpec(aesKey, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao cifrar segredo de integração", ex);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Falha ao decifrar segredo de integração", ex);
        }
    }

    private static byte[] decodeKey(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }
}
