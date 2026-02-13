package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String phone, @NotBlank String otp, @NotBlank String newPassword) {}
