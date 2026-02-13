package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record ManualSourceRequest(@NotBlank String userId, @NotBlank String platform, @NotBlank String nickname, @NotBlank String homepage) {}
