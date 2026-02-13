package com.evomind.api.controller;

import com.evomind.api.model.ApiResponse;
import com.evomind.api.model.CostEstimateRequest;
import com.evomind.api.model.CostEstimateResponse;
import com.evomind.api.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/cost-estimate")
    public ApiResponse<CostEstimateResponse> estimate(@Valid @RequestBody CostEstimateRequest request) {
        return ApiResponse.ok(billingService.estimate(request));
    }
}
