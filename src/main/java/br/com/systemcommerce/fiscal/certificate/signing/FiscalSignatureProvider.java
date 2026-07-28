package br.com.systemcommerce.fiscal.certificate.signing;

import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import java.util.UUID;

public interface FiscalSignatureProvider {

    SignedXmlResult sign(byte[] xmlUtf8, UUID establishmentId, String environment);

    boolean supports(DigitalCertificate.CertificateType type);
}
