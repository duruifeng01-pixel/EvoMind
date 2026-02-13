package com.evomind.api.model;

import java.util.List;

public record PlanItem(String code, String name, String period, int sourceLimit, int discussionLimitDaily, String agentTrainLimit, List<String> features) {}
