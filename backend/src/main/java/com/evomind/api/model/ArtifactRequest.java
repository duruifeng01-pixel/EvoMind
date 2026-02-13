package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record ArtifactRequest(@NotBlank String userId, @NotBlank String type, @NotBlank String content) {}
