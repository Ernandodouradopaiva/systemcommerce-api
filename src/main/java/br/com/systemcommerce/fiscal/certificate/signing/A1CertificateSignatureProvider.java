package br.com.systemcommerce.fiscal.certificate.signing;

import br.com.systemcommerce.fiscal.certificate.entity.CertificateAssignment;
import br.com.systemcommerce.fiscal.certificate.entity.CertificateUsageLog;
import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateAssignmentRepository;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateUsageLogRepository;
import br.com.systemcommerce.fiscal.certificate.repository.DigitalCertificateRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.integration.crypto.SecretEncryptionService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.UUID;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Component
@RequiredArgsConstructor
public class A1CertificateSignatureProvider implements FiscalSignatureProvider {

    private final CertificateAssignmentRepository assignmentRepository;
    private final DigitalCertificateRepository certificateRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final SecretEncryptionService secretEncryptionService;
    private final CertificateUsageLogRepository usageLogRepository;

    @Override
    public boolean supports(DigitalCertificate.CertificateType type) {
        return type == DigitalCertificate.CertificateType.A1;
    }

    @Override
    public SignedXmlResult sign(byte[] xmlUtf8, UUID establishmentId, String environment) {
        FiscalEstablishment establishment = establishmentRepository
                .findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", establishmentId));
        FiscalEstablishment.FiscalEnvironment env = FiscalEstablishment.FiscalEnvironment.valueOf(environment);

        CertificateAssignment assignment = assignmentRepository
                .findByEstablishmentAndEnvironmentAndStatusAndActiveTrue(
                        establishment, env, CertificateAssignment.AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Certificado A1 ativo não encontrado para o estabelecimento"));

        DigitalCertificate certificate = certificateRepository
                .findDetailedById(assignment.getCertificate().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificado digital", assignment.getCertificate().getId()));

        if (certificate.isExpired()) {
            throw new BusinessRuleException("Certificado digital expirado");
        }

        try {
            byte[] keystoreBytes = Base64.getDecoder().decode(secretEncryptionService.decrypt(certificate.getEncryptedKeystore()));
            String password = secretEncryptionService.decrypt(certificate.getEncryptedPassword());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(keystoreBytes), password.toCharArray());
            String alias = firstKeyAlias(keyStore);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            X509Certificate x509 = (X509Certificate) keyStore.getCertificate(alias);

            Document doc = parseXml(xmlUtf8);
            org.w3c.dom.Element signTarget = locateSignTarget(doc);

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            Reference ref = fac.newReference(
                    "",
                    fac.newDigestMethod(DigestMethod.SHA256, null),
                    java.util.List.of(
                            fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                            fac.newTransform("http://www.w3.org/TR/2001/REC-xml-c14n-20010315", (TransformParameterSpec) null)),
                    null,
                    null);
            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    java.util.List.of(ref));
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(java.util.List.of(x509));
            KeyInfo ki = kif.newKeyInfo(java.util.List.of(x509Data));
            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(new DOMSignContext(privateKey, signTarget));

            String signed = documentToString(doc);
            recordUsage(certificate, establishment, "XML_SIGN");
            return new SignedXmlResult(signed.getBytes(StandardCharsets.UTF_8), certificate.getThumbprint(), "A1-" + certificate.getId());
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException("Falha ao assinar XML: " + ex.getMessage());
        }
    }

    private static org.w3c.dom.Element locateSignTarget(Document doc) {
        NodeList infNFe = doc.getElementsByTagName("infNFe");
        if (infNFe.getLength() > 0) {
            return (org.w3c.dom.Element) infNFe.item(0);
        }
        return doc.getDocumentElement();
    }

    private static Document parseXml(byte[] xmlUtf8) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlUtf8));
    }

    private static String documentToString(Document doc) throws Exception {
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static String firstKeyAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        throw new BusinessRuleException("Chave privada não encontrada no certificado");
    }

    private void recordUsage(DigitalCertificate certificate, FiscalEstablishment establishment, String purpose) {
        CertificateUsageLog log = new CertificateUsageLog();
        log.setCertificate(certificate);
        log.setEstablishment(establishment);
        log.setPurpose(purpose);
        CurrentUser.id().ifPresent(log::setPerformedBy);
        usageLogRepository.save(log);
    }
}
