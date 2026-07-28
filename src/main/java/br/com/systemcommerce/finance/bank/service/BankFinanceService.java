package br.com.systemcommerce.finance.bank.service;

import br.com.systemcommerce.finance.bank.dto.BalanceResponse;
import br.com.systemcommerce.finance.bank.dto.BankAccountCreateRequest;
import br.com.systemcommerce.finance.bank.dto.BankAccountResponse;
import br.com.systemcommerce.finance.bank.dto.BankCreateRequest;
import br.com.systemcommerce.finance.bank.dto.BankResponse;
import br.com.systemcommerce.finance.bank.dto.BankUpdateRequest;
import br.com.systemcommerce.finance.bank.dto.FinancialCashCreateRequest;
import br.com.systemcommerce.finance.bank.dto.FinancialCashResponse;
import br.com.systemcommerce.finance.bank.dto.PaymentAccountCreateRequest;
import br.com.systemcommerce.finance.bank.dto.PaymentAccountResponse;
import br.com.systemcommerce.finance.bank.entity.Bank;
import br.com.systemcommerce.finance.bank.entity.BankAccount;
import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialCash;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.entity.PaymentAccount;
import br.com.systemcommerce.finance.bank.repository.BankAccountRepository;
import br.com.systemcommerce.finance.bank.repository.BankRepository;
import br.com.systemcommerce.finance.bank.repository.FinancialAccountHolderRepository;
import br.com.systemcommerce.finance.bank.repository.FinancialCashRepository;
import br.com.systemcommerce.finance.bank.repository.FinancialHolderMovementRepository;
import br.com.systemcommerce.finance.bank.repository.PaymentAccountRepository;
import br.com.systemcommerce.finance.security.FinanceAuditEvents;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BankFinanceService {

    private final BankRepository bankRepository;
    private final FinancialAccountHolderRepository holderRepository;
    private final BankAccountRepository bankAccountRepository;
    private final FinancialCashRepository financialCashRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final FinancialHolderMovementRepository movementRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final PosTerminalRepository posTerminalRepository;
    private final DomainAuditService domainAuditService;
    private final FinanceAuditService financeAuditService;

    // --- Banks ---
    @Transactional(readOnly = true)
    public Page<BankResponse> listBanks(UUID organizationId, Pageable pageable) {
        Specification<Bank> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return bankRepository.findAll(spec, pageable).map(this::toBankResponse);
    }

    @Transactional
    public BankResponse createBank(BankCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (bankRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe banco com este código");
        }
        Bank bank = new Bank();
        bank.setOrganization(org);
        bank.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        bank.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        bank.setShortName(MoneyAndQuantityUtils.blankToNull(request.shortName()));
        bank.setCountryCode(StringUtils.hasText(request.countryCode()) ? request.countryCode() : "BR");
        bank.setStatus(Bank.BankStatus.ACTIVE);
        Bank saved = bankRepository.save(bank);
        domainAuditService.record("FINANCE", "Bank", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Banco criado");
        return toBankResponse(saved);
    }

    @Transactional
    public BankResponse updateBank(UUID id, BankUpdateRequest request) {
        Bank bank = bankRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Banco não encontrado"));
        if (bankRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                bank.getOrganization().getId(), request.code(), id)) {
            throw new ConflictException("Já existe banco com este código");
        }
        bank.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        bank.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        bank.setShortName(MoneyAndQuantityUtils.blankToNull(request.shortName()));
        if (StringUtils.hasText(request.countryCode())) {
            bank.setCountryCode(request.countryCode());
        }
        return toBankResponse(bankRepository.save(bank));
    }

    @Transactional
    public BankResponse activateBank(UUID id) {
        Bank bank = bankRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Banco não encontrado"));
        bank.markActive();
        return toBankResponse(bankRepository.save(bank));
    }

    @Transactional
    public BankResponse inactivateBank(UUID id) {
        Bank bank = bankRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Banco não encontrado"));
        bank.markInactive();
        return toBankResponse(bankRepository.save(bank));
    }

    // --- Bank accounts ---
    @Transactional(readOnly = true)
    public List<BankAccountResponse> listBankAccounts(UUID organizationId) {
        return bankAccountRepository.findByOrganizationId(organizationId).stream()
                .map(this::toBankAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getBankAccount(UUID id) {
        return toBankAccountResponse(bankAccountRepository
                .findDetailedByHolderId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária não encontrada")));
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(UUID holderId) {
        holderRepository
                .findById(holderId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta financeira não encontrada"));
        BalanceResponse response = new BalanceResponse(holderId, computeBalance(holderId), Instant.now());
        financeAuditService.success(
                FinanceAuditEvents.BALANCE_ACCESS,
                "FinancialAccountHolder",
                holderId,
                AuditLog.AuditAction.OTHER,
                "Consulta de saldo");
        return response;
    }

    @Transactional
    public BankAccountResponse createBankAccount(BankAccountCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (holderRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe instrumento financeiro com este código");
        }
        Bank bank = bankRepository
                .findById(request.bankId())
                .orElseThrow(() -> new ResourceNotFoundException("Banco não encontrado"));
        if (!bank.isUsable()) {
            throw new BusinessRuleException("Banco inativo não pode ser utilizado");
        }

        FinancialAccountHolder holder = newHolder(
                org,
                request.storeId(),
                mapHolderType(request.accountKind()),
                request.code(),
                request.name(),
                request.currency(),
                request.openingBalance(),
                request.openingBalanceDate(),
                request.allowsPayments(),
                request.allowsReceipts(),
                request.allowsReconciliation());
        holder = holderRepository.save(holder);
        postOpeningBalance(holder);

        BankAccount account = new BankAccount();
        account.setHolder(holder);
        account.setBank(bank);
        account.setAgency(MoneyAndQuantityUtils.requireText(request.agency(), "Agência"));
        account.setAccountNumber(MoneyAndQuantityUtils.requireText(request.accountNumber(), "Número"));
        account.setAccountDigit(MoneyAndQuantityUtils.blankToNull(request.accountDigit()));
        account.setAccountKind(request.accountKind());
        account.setHolderName(MoneyAndQuantityUtils.requireText(request.holderName(), "Titular"));
        account.setHolderDocument(MoneyAndQuantityUtils.blankToNull(request.holderDocument()));
        bankAccountRepository.save(account);

        domainAuditService.record(
                "FINANCE", "BankAccount", holder.getId(), AuditLog.AuditAction.CREATE, null, null, "Conta bancária criada");
        return getBankAccount(holder.getId());
    }

    @Transactional
    public BankAccountResponse activateBankAccount(UUID id) {
        FinancialAccountHolder holder = requireHolder(id);
        holder.markActive();
        holderRepository.save(holder);
        return getBankAccount(id);
    }

    @Transactional
    public BankAccountResponse inactivateBankAccount(UUID id) {
        FinancialAccountHolder holder = requireHolder(id);
        holder.markInactive();
        holderRepository.save(holder);
        return getBankAccount(id);
    }

    // --- Financial cash ---
    @Transactional(readOnly = true)
    public List<FinancialCashResponse> listCashes(UUID organizationId) {
        return financialCashRepository.findByOrganizationId(organizationId).stream()
                .map(this::toCashResponse)
                .toList();
    }

    @Transactional
    public FinancialCashResponse createCash(FinancialCashCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (holderRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe instrumento financeiro com este código");
        }
        FinancialAccountHolder.HolderType type = request.cashKind() == FinancialCash.CashKind.POS
                ? FinancialAccountHolder.HolderType.POS_CASH
                : FinancialAccountHolder.HolderType.ADMIN_CASH;
        FinancialAccountHolder holder = newHolder(
                org,
                request.storeId(),
                type,
                request.code(),
                request.name(),
                "BRL",
                request.openingBalance(),
                request.openingBalanceDate(),
                true,
                true,
                false);
        holder = holderRepository.save(holder);
        postOpeningBalance(holder);

        FinancialCash cash = new FinancialCash();
        cash.setHolder(holder);
        cash.setCashKind(request.cashKind());
        if (request.posTerminalId() != null) {
            PosTerminal terminal = posTerminalRepository
                    .findById(request.posTerminalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Terminal PDV não encontrado"));
            cash.setPosTerminal(terminal);
        }
        financialCashRepository.save(cash);
        domainAuditService.record(
                "FINANCE", "FinancialCash", holder.getId(), AuditLog.AuditAction.CREATE, null, null, "Caixa financeiro criado");
        return toCashResponse(financialCashRepository.findDetailedByHolderId(holder.getId()).orElseThrow());
    }

    @Transactional
    public FinancialCashResponse activateCash(UUID id) {
        FinancialAccountHolder holder = requireHolder(id);
        holder.markActive();
        holderRepository.save(holder);
        return toCashResponse(financialCashRepository.findDetailedByHolderId(id).orElseThrow());
    }

    @Transactional
    public FinancialCashResponse inactivateCash(UUID id) {
        FinancialAccountHolder holder = requireHolder(id);
        holder.markInactive();
        holderRepository.save(holder);
        return toCashResponse(financialCashRepository.findDetailedByHolderId(id).orElseThrow());
    }

    // --- Payment accounts ---
    @Transactional
    public PaymentAccountResponse createPaymentAccount(PaymentAccountCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (holderRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe instrumento financeiro com este código");
        }
        FinancialAccountHolder holder = newHolder(
                org,
                request.storeId(),
                FinancialAccountHolder.HolderType.PAYMENT_ACCOUNT,
                request.code(),
                request.name(),
                "BRL",
                request.openingBalance(),
                request.openingBalanceDate(),
                true,
                true,
                true);
        holder = holderRepository.save(holder);
        postOpeningBalance(holder);

        PaymentAccount account = new PaymentAccount();
        account.setHolder(holder);
        account.setProviderCode(MoneyAndQuantityUtils.requireText(request.providerCode(), "Provedor"));
        account.setProviderName(MoneyAndQuantityUtils.blankToNull(request.providerName()));
        account.setExternalAccountId(MoneyAndQuantityUtils.blankToNull(request.externalAccountId()));
        paymentAccountRepository.save(account);
        return toPaymentAccountResponse(paymentAccountRepository.findDetailedByHolderId(holder.getId()).orElseThrow());
    }

    public BigDecimal computeBalance(UUID holderId) {
        return movementRepository.sumActiveAmount(holderId).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Registra saída (PAYMENT negativo) ou entrada (RECEIPT positivo) no holder.
     * Saldo atual = soma das movimentações ativas; nunca é editado diretamente.
     */
    @Transactional
    public FinancialHolderMovement postMovement(
            UUID holderId,
            FinancialHolderMovement.MovementType type,
            BigDecimal signedAmount,
            String description,
            String sourceDocumentType,
            UUID sourceDocumentId) {
        FinancialAccountHolder holder = requireHolder(holderId);
        if (!holder.isUsable()) {
            throw new BusinessRuleException("Conta bancária/caixa inativa não pode receber movimentação");
        }
        if (type == FinancialHolderMovement.MovementType.PAYMENT
                && !Boolean.TRUE.equals(holder.getAllowsPayments())) {
            throw new BusinessRuleException("Instrumento financeiro não permite pagamentos");
        }
        if (type == FinancialHolderMovement.MovementType.RECEIPT
                && !Boolean.TRUE.equals(holder.getAllowsReceipts())) {
            throw new BusinessRuleException("Instrumento financeiro não permite recebimentos");
        }
        BigDecimal amount = signedAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = computeBalance(holderId).add(amount);
        FinancialHolderMovement movement = new FinancialHolderMovement();
        movement.setHolder(holder);
        movement.setMovementType(type);
        movement.setAmount(amount);
        movement.setBalanceAfter(balanceAfter);
        movement.setOccurredAt(Instant.now());
        movement.setDescription(description);
        movement.setSourceDocumentType(sourceDocumentType);
        movement.setSourceDocumentId(sourceDocumentId);
        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public FinancialAccountHolder requireUsableHolder(UUID holderId) {
        FinancialAccountHolder holder = requireHolder(holderId);
        if (!holder.isUsable()) {
            throw new BusinessRuleException("Instrumento financeiro inativo");
        }
        return holder;
    }

    /**
     * Resolve o caixa PDV para liquidação automática: sessão vinculada, depois caixa POS da loja.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> resolvePosCashHolderId(UUID organizationId, UUID storeId, UUID cashSessionId) {
        if (cashSessionId != null) {
            Optional<UUID> linked = financialCashRepository
                    .findByLinkedCashSessionId(cashSessionId)
                    .map(c -> c.getHolder().getId());
            if (linked.isPresent()) {
                return linked;
            }
        }
        if (organizationId == null || storeId == null) {
            return Optional.empty();
        }
        return financialCashRepository
                .findActivePosByStore(organizationId, storeId)
                .stream()
                .findFirst()
                .map(c -> c.getHolder().getId());
    }

    private void postOpeningBalance(FinancialAccountHolder holder) {
        BigDecimal opening = holder.getOpeningBalance() != null ? holder.getOpeningBalance() : BigDecimal.ZERO;
        FinancialHolderMovement movement = new FinancialHolderMovement();
        movement.setHolder(holder);
        movement.setMovementType(FinancialHolderMovement.MovementType.OPENING_BALANCE);
        movement.setAmount(opening);
        movement.setBalanceAfter(opening);
        movement.setOccurredAt(Instant.now());
        movement.setDescription("Saldo inicial formal");
        movementRepository.save(movement);
    }

    private FinancialAccountHolder newHolder(
            Organization org,
            UUID storeId,
            FinancialAccountHolder.HolderType type,
            String code,
            String name,
            String currency,
            BigDecimal openingBalance,
            java.time.LocalDate openingDate,
            Boolean allowsPayments,
            Boolean allowsReceipts,
            Boolean allowsReconciliation) {
        FinancialAccountHolder holder = new FinancialAccountHolder();
        holder.setOrganization(org);
        if (storeId != null) {
            holder.setStore(storeService.requireUsable(storeId));
        }
        holder.setHolderType(type);
        holder.setCode(MoneyAndQuantityUtils.requireText(code, "Código"));
        holder.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        holder.setCurrency(StringUtils.hasText(currency) ? currency : "BRL");
        holder.setOpeningBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO);
        holder.setOpeningBalanceDate(openingDate);
        holder.setAllowsPayments(allowsPayments == null || allowsPayments);
        holder.setAllowsReceipts(allowsReceipts == null || allowsReceipts);
        holder.setAllowsReconciliation(Boolean.TRUE.equals(allowsReconciliation));
        holder.setStatus(FinancialAccountHolder.HolderStatus.ACTIVE);
        return holder;
    }

    private FinancialAccountHolder requireHolder(UUID id) {
        return holderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrumento financeiro não encontrado"));
    }

    private FinancialAccountHolder.HolderType mapHolderType(BankAccount.AccountKind kind) {
        return switch (kind) {
            case SAVINGS -> FinancialAccountHolder.HolderType.SAVINGS;
            case PAYMENT -> FinancialAccountHolder.HolderType.PAYMENT_ACCOUNT;
            default -> FinancialAccountHolder.HolderType.CHECKING;
        };
    }

    private BankResponse toBankResponse(Bank bank) {
        return new BankResponse(
                bank.getId(),
                bank.getOrganization().getId(),
                bank.getCode(),
                bank.getName(),
                bank.getShortName(),
                bank.getCountryCode(),
                bank.getStatus(),
                bank.isUsable(),
                bank.getVersion());
    }

    private BankAccountResponse toBankAccountResponse(BankAccount account) {
        FinancialAccountHolder h = account.getHolder();
        return new BankAccountResponse(
                h.getId(),
                h.getOrganization().getId(),
                h.getStore() != null ? h.getStore().getId() : null,
                h.getCode(),
                h.getName(),
                account.getBank().getId(),
                account.getBank().getCode(),
                account.getBank().getName(),
                account.getAgency(),
                account.getAccountNumber(),
                account.getAccountDigit(),
                account.getAccountKind(),
                account.getHolderName(),
                account.getHolderDocument(),
                h.getCurrency(),
                h.getOpeningBalance(),
                h.getOpeningBalanceDate(),
                computeBalance(h.getId()),
                Boolean.TRUE.equals(h.getAllowsPayments()),
                Boolean.TRUE.equals(h.getAllowsReceipts()),
                Boolean.TRUE.equals(h.getAllowsReconciliation()),
                h.getStatus(),
                h.isUsable(),
                h.getVersion());
    }

    private FinancialCashResponse toCashResponse(FinancialCash cash) {
        FinancialAccountHolder h = cash.getHolder();
        return new FinancialCashResponse(
                h.getId(),
                h.getOrganization().getId(),
                h.getStore() != null ? h.getStore().getId() : null,
                h.getCode(),
                h.getName(),
                cash.getCashKind(),
                cash.getPosTerminal() != null ? cash.getPosTerminal().getId() : null,
                cash.getLinkedCashSession() != null ? cash.getLinkedCashSession().getId() : null,
                h.getOpeningBalance(),
                computeBalance(h.getId()),
                h.getStatus(),
                h.isUsable(),
                h.getVersion());
    }

    private PaymentAccountResponse toPaymentAccountResponse(PaymentAccount account) {
        FinancialAccountHolder h = account.getHolder();
        return new PaymentAccountResponse(
                h.getId(),
                h.getOrganization().getId(),
                h.getCode(),
                h.getName(),
                account.getProviderCode(),
                account.getProviderName(),
                account.getExternalAccountId(),
                computeBalance(h.getId()),
                h.getStatus(),
                h.isUsable(),
                h.getVersion());
    }
}
