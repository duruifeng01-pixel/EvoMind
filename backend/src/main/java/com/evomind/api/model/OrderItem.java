package com.evomind.api.model;

public record OrderItem(String orderNo, String userId, String planCode, String channel, int amount, String status, String createdAt) {}
