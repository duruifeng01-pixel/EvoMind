package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SourceImportRequest(
        @NotBlank String userId,
        @NotBlank String platform,
        @NotEmpty List<Item> items
) {
    public record Item(@NotBlank String nickname, @NotBlank String homepage) {}
}
