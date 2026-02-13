package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(
        @NotBlank String userId,
        @NotBlank String planCode,
        @NotBlank String channel,
        int amount
) {}
