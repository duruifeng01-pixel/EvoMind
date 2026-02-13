package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record PasswordLoginRequest(@NotBlank String phone, @NotBlank String password) {}
