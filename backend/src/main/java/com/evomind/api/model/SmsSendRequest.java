package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record SmsSendRequest(@NotBlank String phone) {}
