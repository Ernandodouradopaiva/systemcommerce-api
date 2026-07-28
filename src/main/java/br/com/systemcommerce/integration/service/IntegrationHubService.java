package br.com.systemcommerce.integration.service;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.crypto.SecretEncryptionService;
import br.com.systemcommerce.integration.dto.ChannelProductLinkRequest;
import br.com.systemcommerce.integration.dto.ChannelProductResponse;
import br.com.systemcommerce.integration.dto.MarketplaceAccountCreateRequest;
import br.com.systemcommerce.integration.dto.MarketplaceAccountResponse;
import br.com.systemcommerce.integration.dto.SalesChannelCreateRequest;
import br.com.systemcommerce.integration.dto.SalesChannelResponse;
import br.com.systemcommerce.integration.entity.ChannelProduct;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import br.com.systemcommerce.integration.entity.MarketplaceAccountStatus;
import br.com.systemcommerce.integration.entity.SalesChannel;
import br.com.systemcommerce.integration.repository.ChannelProductRepository;
import br.com.systemcommerce.integration.repository.MarketplaceAccountRepository;
import br.com.systemcommerce.integration.repository.SalesChannelRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IntegrationHubService {

    private final SalesChannelRepository salesChannelRepository;
    private final MarketplaceAccountRepository marketplaceAccountRepository;
    private final ChannelProductRepository channelProductRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final SecretEncryptionService secretEncryptionService;
    private final DomainAuditService domainAuditService;
    private final List<MarketplaceAdapter> adapters;

    @Transactional(readOnly = true)
    public Page<SalesChannelResponse> listChannels(UUID organizationId, Pageable pageable) {
        return salesChannelRepository
                .findAll(
                        (root, q, cb) ->
                                organizationId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(this::toChannelResponse);
    }

    @Transactional
    public SalesChannelResponse createChannel(SalesChannelCreateRequest request) {
        Organization org = organizationService.resolveForStoreCreate(request.organizationId());
        salesChannelRepository
                .findByOrganizationIdAndCode(org.getId(), request.code().trim())
                .ifPresent(c -> {
                    throw new BusinessRuleException("Já existe canal com este código na organização");
                });
        SalesChannel channel = new SalesChannel();
        channel.setOrganization(org);
        channel.setCode(request.code().trim().toUpperCase());
        channel.setName(request.name().trim());
        channel.setChannelType(request.channelType());
        SalesChannel saved = salesChannelRepository.save(channel);
        domainAuditService.record(
                "SalesChannel",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("code", saved.getCode(), "type", saved.getChannelType().name()),
                "Canal de venda criado");
        return toChannelResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MarketplaceAccountResponse> listAccounts(UUID organizationId, Pageable pageable) {
        return marketplaceAccountRepository
                .findAll(
                        (root, q, cb) ->
                                organizationId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(this::toAccountResponse);
    }

    @Transactional
    public MarketplaceAccountResponse createAccount(MarketplaceAccountCreateRequest request) {
        Organization org = organizationService.resolveForStoreCreate(request.organizationId());
        SalesChannel channel = salesChannelRepository
                .findById(request.salesChannelId())
                .orElseThrow(() -> new ResourceNotFoundException("Canal não encontrado"));
        if (!channel.getOrganization().getId().equals(org.getId())) {
            throw new BusinessRuleException("Canal não pertence à organização");
        }
        var store = storeService.getEntity(request.storeId());
        var warehouse = warehouseService.getEntity(request.warehouseId());
        MarketplaceAccount account = new MarketplaceAccount();
        account.setOrganization(org);
        account.setSalesChannel(channel);
        account.setStore(store);
        account.setWarehouse(warehouse);
        account.setExternalAccountId(request.externalAccountId());
        account.setDisplayName(request.displayName().trim());
        account.setSettingsJson(request.settingsJson());
        account.setStatus(MarketplaceAccountStatus.ACTIVE);
        String adapter = StringUtils.hasText(request.adapterCode())
                ? request.adapterCode().trim().toUpperCase()
                : channel.getChannelType().name();
        account.setAdapterCode(adapter);
        if (StringUtils.hasText(request.credentialsJson())) {
            account.setCredentialsEncrypted(secretEncryptionService.encrypt(request.credentialsJson()));
        }
        MarketplaceAccount saved = marketplaceAccountRepository.save(account);
        domainAuditService.record(
                "MarketplaceAccount",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("adapter", adapter, "storeId", store.getId().toString()),
                "Conta de marketplace criada");
        return toAccountResponse(saved);
    }

    @Transactional
    public ChannelProductResponse linkProduct(ChannelProductLinkRequest request) {
        MarketplaceAccount account = marketplaceAccountRepository
                .findById(request.marketplaceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta de marketplace não encontrada"));
        var product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        channelProductRepository
                .findByMarketplaceAccountIdAndExternalProductId(
                        account.getId(), request.externalProductId().trim())
                .ifPresent(p -> {
                    throw new BusinessRuleException("Produto externo já vinculado nesta conta");
                });
        ChannelProduct link = new ChannelProduct();
        link.setOrganization(account.getOrganization());
        link.setMarketplaceAccount(account);
        link.setProduct(product);
        link.setExternalProductId(request.externalProductId().trim());
        link.setExternalSku(request.externalSku());
        link.setSyncStatus("LINKED");
        ChannelProduct saved = channelProductRepository.save(link);
        return new ChannelProductResponse(
                saved.getId(),
                account.getId(),
                product.getId(),
                saved.getExternalProductId(),
                saved.getExternalSku(),
                saved.getSyncStatus());
    }

    @Transactional(readOnly = true)
    public MarketplaceAccount getAccountEntity(UUID id) {
        return marketplaceAccountRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta de marketplace não encontrada"));
    }

    public MarketplaceAdapter resolveAdapter(MarketplaceAccount account) {
        String code = StringUtils.hasText(account.getAdapterCode())
                ? account.getAdapterCode()
                : account.getSalesChannel().getChannelType().name();
        Map<String, MarketplaceAdapter> byCode = adapters.stream()
                .collect(Collectors.toMap(MarketplaceAdapter::adapterCode, Function.identity(), (a, b) -> a));
        MarketplaceAdapter adapter = byCode.get(code);
        if (adapter == null) {
            adapter = adapters.stream()
                    .filter(a -> a.supports(code))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Adapter não encontrado para: " + code));
        }
        return adapter;
    }

    public String decryptCredentials(MarketplaceAccount account) {
        return secretEncryptionService.decrypt(account.getCredentialsEncrypted());
    }

    private SalesChannelResponse toChannelResponse(SalesChannel c) {
        return new SalesChannelResponse(
                c.getId(),
                c.getOrganization().getId(),
                c.getCode(),
                c.getName(),
                c.getChannelType(),
                c.getActive());
    }

    private MarketplaceAccountResponse toAccountResponse(MarketplaceAccount a) {
        return new MarketplaceAccountResponse(
                a.getId(),
                a.getOrganization().getId(),
                a.getSalesChannel().getId(),
                a.getStore().getId(),
                a.getWarehouse().getId(),
                a.getExternalAccountId(),
                a.getDisplayName(),
                a.getStatus(),
                a.getAdapterCode(),
                a.getLastSyncAt(),
                StringUtils.hasText(a.getCredentialsEncrypted()));
    }
}
