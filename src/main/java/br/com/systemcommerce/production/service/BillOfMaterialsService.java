package br.com.systemcommerce.production.service;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.production.dto.BillOfMaterialsCreateRequest;
import br.com.systemcommerce.production.dto.BillOfMaterialsResponse;
import br.com.systemcommerce.production.entity.BillOfMaterials;
import br.com.systemcommerce.production.entity.BillOfMaterialsItem;
import br.com.systemcommerce.production.entity.BillOfMaterialsStatus;
import br.com.systemcommerce.production.mapper.ProductionMapper;
import br.com.systemcommerce.production.repository.BillOfMaterialsItemRepository;
import br.com.systemcommerce.production.repository.BillOfMaterialsRepository;
import br.com.systemcommerce.production.specification.BillOfMaterialsSpecifications;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillOfMaterialsService {

    private final BillOfMaterialsRepository bomRepository;
    private final BillOfMaterialsItemRepository itemRepository;
    private final ProductionMapper mapper;
    private final OrganizationService organizationService;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<BillOfMaterialsResponse> list(
            UUID organizationId, UUID finishedProductId, BillOfMaterialsStatus status, String search, Pageable pageable) {
        return bomRepository
                .findAll(BillOfMaterialsSpecifications.withFilters(organizationId, finishedProductId, status, search), pageable)
                .map(bom -> mapper.toResponse(bom, itemRepository.findActiveByBillOfMaterialsId(bom.getId())));
    }

    @Transactional(readOnly = true)
    public BillOfMaterialsResponse getById(UUID id) {
        BillOfMaterials bom = getEntity(id);
        return mapper.toResponse(bom, itemRepository.findActiveByBillOfMaterialsId(id));
    }

    @Transactional
    public BillOfMaterialsResponse create(BillOfMaterialsCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        Product finished = productRepository
                .findById(request.finishedProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto acabado", request.finishedProductId()));

        int version = request.versionNumber() != null ? request.versionNumber() : 1;
        String code = request.code().trim();
        if (bomRepository
                .findByOrganizationIdAndCodeAndVersionNumberAndActiveTrue(organization.getId(), code, version)
                .isPresent()) {
            throw new ConflictException("Ficha técnica já cadastrada para código/versão");
        }

        BillOfMaterials bom = new BillOfMaterials();
        bom.setOrganization(organization);
        bom.setFinishedProduct(finished);
        bom.setCode(code);
        bom.setName(request.name().trim());
        bom.setVersionNumber(version);
        bom.setStatus(BillOfMaterialsStatus.ACTIVE);
        bom.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));

        BillOfMaterials saved = bomRepository.save(bom);
        for (BillOfMaterialsCreateRequest.BomItemRequest line : request.items()) {
            Product component = productRepository
                    .findById(line.componentProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Componente", line.componentProductId()));
            BillOfMaterialsItem item = new BillOfMaterialsItem();
            item.setBillOfMaterials(saved);
            item.setComponentProduct(component);
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(line.quantity()));
            item.setLineNumber(line.lineNumber());
            item.setUnitCode(MoneyAndQuantityUtils.blankToNull(line.unitCode()));
            item.setScrapPercent(line.scrapPercent() != null ? line.scrapPercent() : BigDecimal.ZERO);
            itemRepository.save(item);
        }

        return getById(saved.getId());
    }

    private BillOfMaterials getEntity(UUID id) {
        return bomRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha técnica", id));
    }
}
