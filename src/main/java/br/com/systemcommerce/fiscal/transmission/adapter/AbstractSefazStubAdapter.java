package br.com.systemcommerce.fiscal.transmission.adapter;

import br.com.systemcommerce.fiscal.config.FiscalProperties;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.transmission.dto.AuthorizationResult;
import br.com.systemcommerce.fiscal.transmission.dto.DistributionResult;
import br.com.systemcommerce.fiscal.transmission.dto.EventResult;
import br.com.systemcommerce.fiscal.transmission.dto.NsuDistributionResult;
import br.com.systemcommerce.fiscal.transmission.dto.ProtocolResult;
import br.com.systemcommerce.fiscal.transmission.dto.ReceiptResult;
import br.com.systemcommerce.fiscal.transmission.dto.ServiceStatusResult;
import br.com.systemcommerce.fiscal.transmission.dto.SignedXmlPayload;
import br.com.systemcommerce.fiscal.transmission.dto.VoidingResult;
import br.com.systemcommerce.fiscal.transmission.service.FiscalCircuitBreaker;
import br.com.systemcommerce.fiscal.transmission.service.FiscalEndpointRegistryService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public abstract class AbstractSefazStubAdapter implements FiscalAuthorityAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final FiscalProperties fiscalProperties;
    protected final FiscalEndpointRegistryService endpointRegistry;
    protected final FiscalCircuitBreaker circuitBreaker;
    protected final FiscalEstablishmentRepository establishmentRepository;

    protected abstract String modelCode();

    @Override
    public ServiceStatusResult statusServico(String uf, String model, FiscalEstablishment.FiscalEnvironment environment) {
        if (circuitBreaker.isOpen(
                uf, "statusServico", fiscalProperties.getSefaz().getCircuitBreakerOpenMinutes())) {
            return new ServiceStatusResult(false, "999", "Circuit breaker aberto", 0, true);
        }
        long start = System.currentTimeMillis();
        endpointRegistry.resolveUrl(uf, model, environment, "statusServico");
        long latency = System.currentTimeMillis() - start;
        warnIfProductionStub(environment);
        circuitBreaker.recordSuccess(uf, "statusServico");
        return new ServiceStatusResult(true, "107", "Servico em operacao (stub)", latency, false);
    }

    @Override
    public AuthorizationResult authorize(SignedXmlPayload signedXml, UUID establishmentId, String model) {
        FiscalEstablishment est = requireEstablishment(establishmentId);
        String uf = est.getUf();
        if (circuitBreaker.isOpen(uf, "authorize", fiscalProperties.getSefaz().getCircuitBreakerOpenMinutes())) {
            return new AuthorizationResult(false, "999", "Circuit breaker aberto", null, null, null, 0);
        }
        long start = System.currentTimeMillis();
        endpointRegistry.resolveUrl(uf, model, est.getFiscalEnvironment(), "authorize");
        long latency = System.currentTimeMillis() - start;
        warnIfProductionStub(est.getFiscalEnvironment());

        if (shouldStub(est.getFiscalEnvironment())) {
            String protocol = "STUB" + System.currentTimeMillis();
            String xml = new String(signedXml.xmlUtf8(), StandardCharsets.UTF_8);
            String authorized = xml.replaceFirst(
                    "</NFe>",
                    "<protNFe><infProt><nProt>" + protocol + "</nProt><cStat>100</cStat></infProt></protNFe></NFe>");
            circuitBreaker.recordSuccess(uf, "authorize");
            return new AuthorizationResult(true, "100", "Autorizado (stub)", protocol, protocol, authorized, latency);
        }
        circuitBreaker.recordFailure(
                uf, "authorize", fiscalProperties.getSefaz().getCircuitBreakerFailureThreshold(),
                fiscalProperties.getSefaz().getCircuitBreakerOpenMinutes());
        return new AuthorizationResult(false, "999", "Transmissão real não implementada", null, null, null, latency);
    }

    @Override
    public ReceiptResult consultaRecibo(String receiptNumber, UUID establishmentId, String model) {
        FiscalEstablishment est = requireEstablishment(establishmentId);
        long latency = 5;
        if (shouldStub(est.getFiscalEnvironment())) {
            return new ReceiptResult(true, "104", "Lote processado (stub)", "STUB-PROT-" + receiptNumber, latency);
        }
        return new ReceiptResult(false, "999", "Consulta recibo não implementada", null, latency);
    }

    @Override
    public ProtocolResult consultaProtocolo(String accessKey, UUID establishmentId, String model) {
        FiscalEstablishment est = requireEstablishment(establishmentId);
        if (shouldStub(est.getFiscalEnvironment())) {
            return new ProtocolResult(true, "100", "Autorizado (stub consulta)", "STUB-PROT", accessKey, 10);
        }
        return new ProtocolResult(false, "217", "NF-e não consta na base", null, accessKey, 10);
    }

    @Override
    public EventResult sendEvent(byte[] eventXmlUtf8, UUID establishmentId, String model, String eventType) {
        FiscalEstablishment est = requireEstablishment(establishmentId);
        if (shouldStub(est.getFiscalEnvironment())) {
            String protocol = "EVT" + System.currentTimeMillis();
            return new EventResult(true, "135", "Evento registrado (stub)", protocol,
                    new String(eventXmlUtf8, StandardCharsets.UTF_8), 15);
        }
        return new EventResult(false, "999", "Evento não implementado", null, null, 15);
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
        FiscalEstablishment est = requireEstablishment(establishmentId);
        if (shouldStub(environment)) {
            String protocol = "INUT" + System.currentTimeMillis();
            return new VoidingResult(true, "102", "Inutilização homologada (stub)", protocol,
                    "<inutNFe stub=\"true\"/>", 20);
        }
        warnIfProductionStub(environment);
        return new VoidingResult(false, "999", "Inutilização real não implementada", null, null, 20);
    }

    @Override
    public DistributionResult distribuicao(String accessKey, UUID establishmentId, String model) {
        if (shouldStub(requireEstablishment(establishmentId).getFiscalEnvironment())) {
            return new DistributionResult(true, "138", "Documento localizado (stub)", 12);
        }
        return new DistributionResult(false, "999", "Distribuição não implementada", 12);
    }

    @Override
    public NsuDistributionResult distribuicaoPorNsu(UUID establishmentId, String uf, long lastNsu) {
        FiscalEstablishment est = requireEstablishment(establishmentId);
        if (shouldStub(est.getFiscalEnvironment())) {
            // Sem documentos novos — cStat 137 (nenhum documento)
            return new NsuDistributionResult(true, "137", "Nenhum documento localizado (stub)", 15, lastNsu, lastNsu, List.of());
        }
        return new NsuDistributionResult(false, "999", "Distribuição NSU não implementada", 15, lastNsu, null, List.of());
    }

    protected boolean shouldStub(FiscalEstablishment.FiscalEnvironment environment) {
        if (fiscalProperties.getSefaz().isStub()) {
            return true;
        }
        return environment == FiscalEstablishment.FiscalEnvironment.HOMOLOGATION;
    }

    protected void warnIfProductionStub(FiscalEstablishment.FiscalEnvironment environment) {
        if (environment == FiscalEstablishment.FiscalEnvironment.PRODUCTION && fiscalProperties.getSefaz().isStub()) {
            log.warn("SEFAZ stub ativo em ambiente PRODUCTION para modelo {}", modelCode());
        }
    }

    protected FiscalEstablishment requireEstablishment(UUID id) {
        return establishmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", id));
    }
}
