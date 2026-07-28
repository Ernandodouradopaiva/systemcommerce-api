package br.com.systemcommerce.report.support;

/**
 * Escopo de consulta para relatórios/dashboard ERP.
 * STORE — uma loja; MULTI — lojas acessíveis; GLOBAL — todas as lojas (com permissão).
 */
public enum ReportScope {
    STORE,
    MULTI,
    GLOBAL
}
