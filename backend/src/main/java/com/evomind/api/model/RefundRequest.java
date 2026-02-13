package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(@NotBlank String userId, @NotBlank String orderNo, @NotBlank String reason) {}
