package com.evomind.api.model;

import java.util.List;

public record OcrRecognizeResponse(int latencyMs, List<SourceCandidate> candidates, String note) {
    public record SourceCandidate(String nickname, String homepage) {}
}
