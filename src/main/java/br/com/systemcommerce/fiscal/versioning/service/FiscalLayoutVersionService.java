package br.com.systemcommerce.fiscal.versioning.service;

import br.com.systemcommerce.fiscal.versioning.entity.FiscalLayoutVersion;
import br.com.systemcommerce.fiscal.versioning.entity.FiscalTaxRuleSetVersion;
import br.com.systemcommerce.fiscal.versioning.repository.FiscalLayoutVersionRepository;
import br.com.systemcommerce.fiscal.versioning.repository.FiscalTaxRuleSetVersionRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalLayoutVersionService {

    private final FiscalLayoutVersionRepository layoutVersionRepository;
    private final FiscalTaxRuleSetVersionRepository ruleSetRepository;

    @Transactional(readOnly = true)
    public FiscalLayoutVersion resolveForEmission(String model, LocalDate issueDate) {
        List<FiscalLayoutVersion> list = layoutVersionRepository.findValidOn(model, issueDate);
        if (list.isEmpty()) {
            throw new BusinessRuleException("Nenhuma versão de leiaute válida para " + model + " em " + issueDate);
        }
        return list.get(0);
    }

    @Transactional(readOnly = true)
    public List<FiscalLayoutVersion> listVersions() {
        return layoutVersionRepository.findAll();
    }

    @Transactional
    public FiscalLayoutVersion registerNtUpdate(String ntCode, String description, LocalDate validFrom) {
        String code = "NFe_4.00_NT" + ntCode;
        return layoutVersionRepository.findByCode(code).orElseGet(() -> {
            FiscalLayoutVersion v = new FiscalLayoutVersion();
            v.setCode(code);
            v.setDescription(description);
            v.setModel("ALL");
            v.setSchemaNamespace("http://www.portalfiscal.inf.br/nfe");
            v.setValidFrom(validFrom);
            v.setFeatureFlagsJson("{\"ibs\":true,\"cbs\":true,\"is\":true,\"nt\":\"" + ntCode + "\"}");
            return layoutVersionRepository.save(v);
        });
    }

    @Transactional
    public FiscalTaxRuleSetVersion lockRuleSet(UUID id) {
        FiscalTaxRuleSetVersion rs = ruleSetRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule set", id));
        rs.setLocked(true);
        return ruleSetRepository.save(rs);
    }

    @Transactional
    public FiscalTaxRuleSetVersion updateRules(UUID id, String rulesJson) {
        FiscalTaxRuleSetVersion rs = ruleSetRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule set", id));
        if (Boolean.TRUE.equals(rs.getLocked())) {
            throw new BusinessRuleException("Rule set bloqueado — parâmetros não podem ser alterados retroativamente");
        }
        rs.setRulesJson(rulesJson);
        return ruleSetRepository.save(rs);
    }
}
