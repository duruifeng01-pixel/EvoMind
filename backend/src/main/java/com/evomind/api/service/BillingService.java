package com.evomind.api.service;

import com.evomind.api.model.CostEstimateRequest;
import com.evomind.api.model.CostEstimateResponse;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    // 示例单价（元/单位），上线时改为数据库配置
    private static final double PRICE_SOURCE = 0.03;
    private static final double PRICE_CONFLICT = 0.06;
    private static final double PRICE_TOKEN = 0.0004;
    private static final double PRICE_DISCUSSION = 0.08;
    private static final double PRICE_AGENT_TRAIN = 1.20;

    public CostEstimateResponse estimate(CostEstimateRequest req) {
        double realtimeCost =
                req.sourceCount() * PRICE_SOURCE +
                req.conflictCheckCount() * PRICE_CONFLICT +
                req.summaryTokens() * PRICE_TOKEN +
                req.discussionRounds() * PRICE_DISCUSSION +
                req.agentTrainCount() * PRICE_AGENT_TRAIN;

        int roundedCost = (int) Math.ceil(realtimeCost);
        int subscriptionFee = roundedCost * 2;

        return new CostEstimateResponse(
                Math.round(realtimeCost * 100.0) / 100.0,
                roundedCost,
                subscriptionFee,
                "订阅费 = 向上取整(实时算力成本) × 2",
                "费用构成：80%算力成本 + 20%运营成本"
        );
    }
}
