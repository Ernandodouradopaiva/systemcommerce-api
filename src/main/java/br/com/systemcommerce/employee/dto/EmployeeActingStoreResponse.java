package br.com.systemcommerce.employee.dto;

import java.util.UUID;

public record EmployeeActingStoreResponse(
        UUID storeId,
        String storeCode,
        String storeName,
        boolean primary,
        String storeRole,
        UUID assignmentId) {}
