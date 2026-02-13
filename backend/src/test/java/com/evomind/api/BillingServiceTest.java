package com.evomind.api;

import com.evomind.api.model.CostEstimateRequest;
import com.evomind.api.service.BillingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BillingServiceTest {
    @Test
    void estimateShouldFollowFormula() {
        BillingService service = new BillingService();
        var response = service.estimate(new CostEstimateRequest(10, 3, 1000, 6, 1));

        Assertions.assertTrue(response.realtimeCost() > 0);
        Assertions.assertEquals((int)Math.ceil(response.realtimeCost()), response.roundedCost());
        Assertions.assertEquals(response.roundedCost() * 2, response.subscriptionFee());
    }
}
