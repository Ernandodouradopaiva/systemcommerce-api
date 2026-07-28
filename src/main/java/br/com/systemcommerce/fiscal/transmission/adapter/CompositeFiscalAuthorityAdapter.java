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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class CompositeFiscalAuthorityAdapter implements FiscalAuthorityAdapter {

    private final SefazNfeAdapter nfeAdapter;
    private final SefazNfceAdapter nfceAdapter;

    @Override
    public ServiceStatusResult statusServico(String uf, String model, FiscalEstablishment.FiscalEnvironment environment) {
        return delegate(model).statusServico(uf, model, environment);
    }

    @Override
    public AuthorizationResult authorize(SignedXmlPayload signedXml, UUID establishmentId, String model) {
        return delegate(model).authorize(signedXml, establishmentId, model);
    }

    @Override
    public ReceiptResult consultaRecibo(String receiptNumber, UUID establishmentId, String model) {
        return delegate(model).consultaRecibo(receiptNumber, establishmentId, model);
    }

    @Override
    public ProtocolResult consultaProtocolo(String accessKey, UUID establishmentId, String model) {
        return delegate(model).consultaProtocolo(accessKey, establishmentId, model);
    }

    @Override
    public EventResult sendEvent(byte[] eventXmlUtf8, UUID establishmentId, String model, String eventType) {
        return delegate(model).sendEvent(eventXmlUtf8, establishmentId, model, eventType);
    }

    @Override
    public VoidingResult inutilizar(
            UUID establishmentId,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment,
            long fromNumber,
            long toNumber,
            String justification) {
        return delegate(model).inutilizar(establishmentId, model, series, environment, fromNumber, toNumber, justification);
    }

    @Override
    public DistributionResult distribuicao(String accessKey, UUID establishmentId, String model) {
        return delegate(model).distribuicao(accessKey, establishmentId, model);
    }

    @Override
    public NsuDistributionResult distribuicaoPorNsu(UUID establishmentId, String uf, long lastNsu) {
        return nfeAdapter.distribuicaoPorNsu(establishmentId, uf, lastNsu);
    }

    private FiscalAuthorityAdapter delegate(String model) {
        if ("65".equals(model)) {
            return nfceAdapter;
        }
        return nfeAdapter;
    }
}
