package br.com.systemcommerce.fiscal.transmission.adapter;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.transmission.dto.AuthorizationResult;
import br.com.systemcommerce.fiscal.transmission.dto.DistributionResult;
import br.com.systemcommerce.fiscal.transmission.dto.EventResult;
import br.com.systemcommerce.fiscal.transmission.dto.NsuDistributionResult;
import br.com.systemcommerce.fiscal.transmission.dto.ProtocolResult;
import br.com.systemcommerce.fiscal.transmission.dto.ReceiptResult;
import br.com.systemcommerce.fiscal.transmission.dto.ServiceStatusResult;
import br.com.systemcommerce.fiscal.transmission.dto.SignedXmlPayload;
import br.com.systemcommerce.fiscal.transmission.dto.VoidingResult;
import java.util.UUID;

public interface FiscalAuthorityAdapter {

    ServiceStatusResult statusServico(String uf, String model, FiscalEstablishment.FiscalEnvironment environment);

    AuthorizationResult authorize(SignedXmlPayload signedXml, UUID establishmentId, String model);

    ReceiptResult consultaRecibo(String receiptNumber, UUID establishmentId, String model);

    ProtocolResult consultaProtocolo(String accessKey, UUID establishmentId, String model);

    EventResult sendEvent(byte[] eventXmlUtf8, UUID establishmentId, String model, String eventType);

    VoidingResult inutilizar(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment,
            long fromNumber,
            long toNumber,
            String justification);

    DistributionResult distribuicao(String accessKey, UUID establishmentId, String model);

    /** Busca incremental por NSU (NFeDistribuicaoDFe). */
    NsuDistributionResult distribuicaoPorNsu(UUID establishmentId, String uf, long lastNsu);
}
