package br.com.systemcommerce.finance.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalPolicy;
import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import br.com.systemcommerce.finance.approval.repository.FinancialApprovalPolicyRepository;
import br.com.systemcommerce.finance.approval.repository.FinancialApprovalRequestRepository;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialApprovalServiceTest {

    @Mock private FinancialApprovalPolicyRepository policyRepository;
    @Mock private FinancialApprovalRequestRepository requestRepository;
    @Mock private OrganizationService organizationService;
    @Mock private StoreService storeService;
    @Mock private FinanceAuditService financeAuditService;

    private FinancialApprovalService service;
    private UUID orgId;
    private FinancialApprovalPolicy policy;

    @BeforeEach
    void setUp() {
        service = new FinancialApprovalService(
                policyRepository, requestRepository, organizationService, storeService, financeAuditService);
        orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        policy = new FinancialApprovalPolicy();
        policy.setOrganization(org);
        policy.setRequireReversalApproval(true);
        policy.setRequirePaymentApproval(true);
        policy.setPaymentApprovalThreshold(new BigDecimal("1000.00"));
        policy.setRequirePeriodReopenApproval(true);
        when(policyRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(policy));
    }

    @Test
    void reversalAlwaysNeedsApprovalWhenFlagTrue() {
        assertThat(service.needsApproval(orgId, FinancialApprovalRequest.OperationType.REVERSAL, BigDecimal.TEN))
                .isTrue();
    }

    @Test
    void paymentNeedsApprovalOnlyAboveThreshold() {
        assertThat(service.needsApproval(
                        orgId, FinancialApprovalRequest.OperationType.HIGH_PAYMENT, new BigDecimal("999.99")))
                .isFalse();
        assertThat(service.needsApproval(
                        orgId, FinancialApprovalRequest.OperationType.HIGH_PAYMENT, new BigDecimal("1000.00")))
                .isTrue();
    }

    @Test
    void assertApprovedBlocksWithoutRequestWhenRequired() {
        assertThatThrownBy(() -> service.assertApprovedOrNotRequired(
                        orgId, FinancialApprovalRequest.OperationType.PERIOD_REOPEN, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("duas etapas");
    }
}
