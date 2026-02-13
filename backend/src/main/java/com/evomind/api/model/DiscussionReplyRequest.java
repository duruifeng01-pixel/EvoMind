package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record DiscussionReplyRequest(@NotBlank String userId, @NotBlank String answer) {}
