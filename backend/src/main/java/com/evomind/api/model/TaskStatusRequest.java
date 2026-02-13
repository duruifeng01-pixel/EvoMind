package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusRequest(@NotBlank String userId, @NotBlank String status) {}
