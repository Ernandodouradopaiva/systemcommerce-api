package br.com.systemcommerce.fiscal.transmission.dto;

import java.util.List;

public record NsuDistributionResult(
        boolean success,
        String cstat,
        String xmotivo,
        long latencyMs,
        Long ultNsu,
        Long maxNsu,
        List<DistDocItem> documents) {

    public record DistDocItem(long nsu, String schemaType, String accessKey, String xmlContent) {}
}
