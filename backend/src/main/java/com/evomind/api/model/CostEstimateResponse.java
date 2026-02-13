package com.evomind.api.model;

public record CostEstimateResponse(
        double realtimeCost,
        int roundedCost,
        int subscriptionFee,
        String formula,
        String note
) {}
