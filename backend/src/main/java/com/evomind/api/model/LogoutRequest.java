package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String userId) {}
