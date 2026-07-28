package br.com.systemcommerce.webhook.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class WebhookHmacSignerTest {

    @Test
    void signsDeterministically() {
        WebhookHmacSigner signer = new WebhookHmacSigner();
        String a = signer.sign("secret", "{\"ok\":true}");
        String b = signer.sign("secret", "{\"ok\":true}");
        String c = signer.sign("other", "{\"ok\":true}");
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(64, a.length());
    }
}
