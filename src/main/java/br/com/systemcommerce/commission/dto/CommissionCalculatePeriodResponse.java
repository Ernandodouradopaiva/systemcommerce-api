package br.com.systemcommerce.commission.dto;

import java.util.List;

public record CommissionCalculatePeriodResponse(int salesProcessed, int calculationsCreated, List<CommissionCalculationResponse> calculations) {}
