package com.evomind.api.model;

public record SmsSendResponse(String phone, String bizId, String message) {}
