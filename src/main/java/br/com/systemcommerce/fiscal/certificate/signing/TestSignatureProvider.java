package br.com.systemcommerce.fiscal.certificate.signing;

import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TestSignatureProvider implements FiscalSignatureProvider {

    private static final String STUB_THUMBPRINT = "TEST0000000000000000000000000000000000000000";

    @Override
    public boolean supports(DigitalCertificate.CertificateType type) {
        return true;
    }

    @Override
    public SignedXmlResult sign(byte[] xmlUtf8, UUID establishmentId, String environment) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            pair.getPrivate().getAlgorithm(); // ensure key material generated

            String original = new String(xmlUtf8, StandardCharsets.UTF_8);
            String signed = original.replaceFirst(
                    "</NFe>",
                    "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"
                            + "<SignedInfo><SignatureMethod Algorithm=\"RSA-SHA256\"/>"
                            + "<Reference URI=\"\"><DigestValue>TEST-DIGEST</DigestValue></Reference>"
                            + "</SignedInfo>"
                            + "<SignatureValue>TEST-SIGNATURE-" + establishmentId + "-" + environment + "</SignatureValue>"
                            + "</Signature></NFe>");
            return new SignedXmlResult(signed.getBytes(StandardCharsets.UTF_8), STUB_THUMBPRINT, "TEST-SIGN");
        } catch (Exception ex) {
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException("Falha na assinatura de teste: " + ex.getMessage());
        }
    }
}
