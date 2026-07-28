package br.com.systemcommerce.integration.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SecretEncryptionServiceTest {

    @Test
    void encryptDecryptRoundTrip() {
        SecretEncryptionService service =
                new SecretEncryptionService("", "test-secret-key-min-32-bytes-long!!");
        String cipher = service.encrypt("{\"access_token\":\"abc\"}");
        assertNotEquals("{\"access_token\":\"abc\"}", cipher);
        assertEquals("{\"access_token\":\"abc\"}", service.decrypt(cipher));
    }
}
