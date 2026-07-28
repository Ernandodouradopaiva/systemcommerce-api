package br.com.systemcommerce.fiscal.inbound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalImportRequest;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentLinkRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalValidationRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncomingFiscalServiceTest {

    private static final String SAMPLE_XML =
            """
            <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
              <infNFe Id="NFe35250712345678000190550010000000011000000001" versao="4.00">
                <ide><mod>55</mod><serie>1</serie><nNF>1</nNF><dhEmi>2025-07-22T10:00:00-03:00</dhEmi></ide>
                <det><prod><cProd>SKU1</cProd><xProd>Produto</xProd><NCM>12345678</NCM>
                <qCom>1.0000</qCom><vUnCom>10.00</vUnCom><vProd>10.00</vProd></prod></det>
                <Signature>stub</Signature>
              </infNFe>
            </NFe>
            """;

    @Mock
    private IncomingFiscalDocumentRepository documentRepository;

    @Mock
    private IncomingFiscalDocumentItemRepository itemRepository;

    @Mock
    private IncomingFiscalDocumentLinkRepository linkRepository;

    @Mock
    private IncomingFiscalValidationRepository validationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FiscalAuthorityAdapter fiscalAuthorityAdapter;

    @Mock
    private DomainAuditService domainAuditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IncomingFiscalService incomingFiscalService;

    @Test
    void duplicateAccessKeyBlocked() {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(documentRepository.existsByAccessKey(any())).thenReturn(true);

        assertThatThrownBy(() -> incomingFiscalService.importXml(new IncomingFiscalImportRequest(orgId, storeId, SAMPLE_XML)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já importado");
    }

    @Test
    void importsWhenAccessKeyUnique() {
        UUID orgId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        Store store = new Store();
        store.setId(storeId);

        when(documentRepository.existsByAccessKey(any())).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            var doc = inv.getArgument(0, br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument.class);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        var response = incomingFiscalService.importXml(new IncomingFiscalImportRequest(orgId, storeId, SAMPLE_XML));
        assertThat(response.accessKey()).isNotBlank();
    }
}
