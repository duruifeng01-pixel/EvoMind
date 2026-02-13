package com.evomind.api.integration;

import com.evomind.api.model.OcrRecognizeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcrSdkClient {
    public OcrRecognizeResponse recognize(String platform, String imageBase64) {
        // 演示版：后续替换百度OCR/阿里云OCR SDK
        List<OcrRecognizeResponse.SourceCandidate> candidates = List.of(
                new OcrRecognizeResponse.SourceCandidate(platform + "优质博主A", "https://example.cn/a"),
                new OcrRecognizeResponse.SourceCandidate(platform + "优质博主B", "https://example.cn/b")
        );
        return new OcrRecognizeResponse(900, candidates, "AI生成，仅供参考");
    }
}
