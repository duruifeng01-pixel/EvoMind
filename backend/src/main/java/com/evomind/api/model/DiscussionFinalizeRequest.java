package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record DiscussionFinalizeRequest(@NotBlank String userId, @NotBlank String finalAnswer) {}
