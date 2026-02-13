package com.evomind.api.model;

import jakarta.validation.constraints.DecimalMin;

public record CostEstimateRequest(
        @DecimalMin("0.0") double sourceCount,
        @DecimalMin("0.0") double conflictCheckCount,
        @DecimalMin("0.0") double summaryTokens,
        @DecimalMin("0.0") double discussionRounds,
        @DecimalMin("0.0") double agentTrainCount
) {}
