package br.com.systemcommerce.fiscal.document.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentPaymentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.validation.service.FiscalSchemaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XmlGenerationServiceTest {

    @Mock
    private FiscalDocumentItemRepository itemRepository;

    @Mock
    private FiscalDocumentPaymentRepository paymentRepository;

    @Mock
    private FiscalSchemaService schemaService;

    @Mock
    private FiscalDocumentService documentService;

    @InjectMocks
    private XmlGenerationService xmlGenerationService;

    @Test
    void buildXml_generatesNonEmptyStructure() {
        FiscalEstablishment establishment = new FiscalEstablishment();
        establishment.setCnpj("12345678000199");
        establishment.setLegalName("Loja Teste");
        establishment.setUf("CE");

        FiscalDocument document = new FiscalDocument();
        document.setId(UUID.randomUUID());
        document.setModel("55");
        document.setSeries("1");
        document.setNumber(100L);
        document.setEnvironment(FiscalEstablishment.FiscalEnvironment.HOMOLOGATION);
        document.setEstablishment(establishment);
        document.setTotalProducts(BigDecimal.TEN);
        document.setTotalInvoice(BigDecimal.TEN);

        FiscalDocumentItem item = new FiscalDocumentItem();
        item.setLineNumber(1);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.TEN);
        item.setTotalAmount(BigDecimal.TEN);
        item.setActive(true);

        String xml = xmlGenerationService.buildXml(document, List.of(item), List.of(), "4.00");

        assertThat(xml).isNotBlank();
        assertThat(xml).contains("<NFe");
        assertThat(xml).contains("<ide>");
        assertThat(xml).contains("<emit>");
        assertThat(xml).contains("<det");
        assertThat(xml).contains("<total>");
    }
}
