package br.com.systemcommerce.customerstore.service;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.customerstore.dto.CustomerOriginStoreResponse;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipCreateRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipNotesRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipResponse;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationship;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationshipStatus;
import br.com.systemcommerce.customerstore.mapper.CustomerStoreRelationshipMapper;
import br.com.systemcommerce.customerstore.repository.CustomerStoreRelationshipRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerStoreRelationshipService {

    private final CustomerStoreRelationshipRepository relationshipRepository;
    private final CustomerRepository customerRepository;
    private final StoreService storeService;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrganizationService organizationService;
    private final CustomerStoreRelationshipMapper mapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CustomerStoreRelationshipResponse> listByStore(
            UUID storeId, CustomerStoreRelationshipStatus status, Pageable pageable) {
        storeService.requireUsable(storeId);
        return relationshipRepository.findByStoreId(storeId, status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerStoreRelationshipResponse> listByCustomer(
            UUID customerId, CustomerStoreRelationshipStatus status, Pageable pageable) {
        requireCustomer(customerId);
        return relationshipRepository.findByCustomerId(customerId, status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerStoreRelationshipResponse getRelationship(UUID customerId, UUID storeId) {
        return mapper.toResponse(requireRelationship(customerId, storeId));
    }

    @Transactional(readOnly = true)
    public CustomerOriginStoreResponse getOriginStore(UUID customerId) {
        Customer customer = requireCustomer(customerId);
        if (customer.getOriginStore() == null) {
            return new CustomerOriginStoreResponse(customerId, null, null, null);
        }
        Store store = customer.getOriginStore();
        return new CustomerOriginStoreResponse(customerId, store.getId(), store.getCode(), store.getName());
    }

    @Transactional
    public CustomerStoreRelationshipResponse create(UUID customerId, CustomerStoreRelationshipCreateRequest request) {
        Customer customer = requireCustomer(customerId);
        Store store = storeService.requireUsable(request.storeId());

        if (relationshipRepository.existsByCustomerIdAndStoreId(customerId, store.getId())) {
            throw new ConflictException("Cliente já possui vínculo com esta loja");
        }

        ensureCustomerOrganization(customer, store);

        CustomerStoreRelationship relationship = new CustomerStoreRelationship();
        relationship.setCustomer(customer);
        relationship.setStore(store);
        relationship.setFirstServiceAt(Instant.now());
        relationship.setLocalNotes(MoneyAndQuantityUtils.blankToNull(request.localNotes()));
        applyPreferredSeller(relationship, request.preferredSellerProfileId());
        relationship.markActive();

        if (customer.getOriginStore() == null) {
            customer.setOriginStore(store);
            customerRepository.save(customer);
        }

        CustomerStoreRelationship saved = relationshipRepository.save(relationship);
        audit(saved, AuditLog.AuditAction.CREATE, null, "Vínculo cliente-loja criado");
        return mapper.toResponse(saved);
    }

    /** Cria vínculo na criação do cliente (storeId informado no cadastro). */
    @Transactional
    public CustomerStoreRelationship ensureOnCustomerCreate(Customer customer, UUID storeId) {
        if (storeId == null) {
            Organization defaultOrg = organizationService.requireDefault();
            if (customer.getOrganization() == null) {
                customer.setOrganization(defaultOrg);
            }
            return null;
        }

        Store store = storeService.requireUsable(storeId);
        ensureCustomerOrganization(customer, store);
        customer.setOriginStore(store);

        if (relationshipRepository.existsByCustomerIdAndStoreId(customer.getId(), store.getId())) {
            return relationshipRepository
                    .findByCustomerIdAndStoreId(customer.getId(), store.getId())
                    .orElseThrow();
        }

        CustomerStoreRelationship relationship = new CustomerStoreRelationship();
        relationship.setCustomer(customer);
        relationship.setStore(store);
        relationship.setFirstServiceAt(Instant.now());
        relationship.markActive();
        CustomerStoreRelationship saved = relationshipRepository.save(relationship);
        audit(saved, AuditLog.AuditAction.CREATE, null, "Vínculo cliente-loja na criação do cliente");
        return saved;
    }

    @Transactional
    public CustomerStoreRelationshipResponse updateLocalNotes(
            UUID customerId, UUID storeId, CustomerStoreRelationshipNotesRequest request) {
        CustomerStoreRelationship relationship = requireRelationship(customerId, storeId);
        Map<String, Object> before = snapshot(relationship);
        relationship.setLocalNotes(MoneyAndQuantityUtils.blankToNull(request.localNotes()));
        relationship.setCreditLimitOverride(request.creditLimitOverride());
        CustomerStoreRelationship saved = relationshipRepository.save(relationship);
        audit(saved, AuditLog.AuditAction.UPDATE, before, "Notas locais atualizadas");
        return mapper.toResponse(saved);
    }

    private void ensureCustomerOrganization(Customer customer, Store store) {
        if (customer.getOrganization() == null) {
            customer.setOrganization(store.getOrganization());
            customerRepository.save(customer);
            return;
        }
        if (store.getOrganization() != null
                && !customer.getOrganization().getId().equals(store.getOrganization().getId())) {
            throw new BusinessRuleException("Loja não pertence à organização do cliente");
        }
    }

    private void applyPreferredSeller(CustomerStoreRelationship relationship, UUID sellerProfileId) {
        if (sellerProfileId == null) {
            relationship.setPreferredSellerProfile(null);
            return;
        }
        SellerProfile seller = sellerProfileRepository
                .findById(sellerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de vendedor", sellerProfileId));
        if (!seller.isEnabledForSales()) {
            throw new BusinessRuleException("Vendedor preferencial inativo ou indisponível");
        }
        relationship.setPreferredSellerProfile(seller);
    }

    private Customer requireCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", customerId));
    }

    private CustomerStoreRelationship requireRelationship(UUID customerId, UUID storeId) {
        return relationshipRepository
                .findByCustomerIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo cliente-loja", customerId + "/" + storeId));
    }

    private void audit(
            CustomerStoreRelationship relationship,
            AuditLog.AuditAction action,
            Map<String, Object> before,
            String message) {
        domainAuditService.record(
                "CUSTOMER",
                "CustomerStoreRelationship",
                relationship.getId(),
                action,
                before,
                snapshot(relationship),
                message);
    }

    private Map<String, Object> snapshot(CustomerStoreRelationship relationship) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("customerId", relationship.getCustomer().getId());
        map.put("storeId", relationship.getStore().getId());
        map.put("status", relationship.getStatus());
        map.put("localNotes", relationship.getLocalNotes());
        map.put(
                "preferredSellerProfileId",
                relationship.getPreferredSellerProfile() != null
                        ? relationship.getPreferredSellerProfile().getId()
                        : null);
        return map;
    }
}
