package br.com.systemcommerce.fiscal.certificate.signing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestSignatureProviderTest {

    private final TestSignatureProvider provider = new TestSignatureProvider();

    @Test
    void sign_producesDeterministicStubWithoutRealCertificate() {
        byte[] xml = "<NFe><infNFe/></NFe>".getBytes(StandardCharsets.UTF_8);
        SignedXmlResult result = provider.sign(xml, UUID.randomUUID(), "HOMOLOGATION");

        String signed = new String(result.signedXmlUtf8(), StandardCharsets.UTF_8);
        assertThat(signed).contains("<Signature");
        assertThat(signed).contains("TEST-SIGNATURE");
        assertThat(result.thumbprint()).isEqualTo("TEST0000000000000000000000000000000000000000");
        assertThat(provider.supports(br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate.CertificateType.A1))
                .isTrue();
    }
}
