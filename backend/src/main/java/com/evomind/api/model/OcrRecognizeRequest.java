package com.evomind.api.model;

import jakarta.validation.constraints.NotBlank;

public record OcrRecognizeRequest(@NotBlank String imageBase64, @NotBlank String platform) {}
