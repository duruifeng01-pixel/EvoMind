package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record PayCallbackRequest(@NotBlank String orderNo, @NotBlank String tradeNo, @NotBlank String status) {}
