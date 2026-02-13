package com.evomind.api.controller;

import com.evomind.api.integration.OcrSdkClient;
import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    private final InMemoryStore store;
    private final OcrSdkClient ocrSdkClient;

    public SourceController(InMemoryStore store, OcrSdkClient ocrSdkClient) {
        this.store = store;
        this.ocrSdkClient = ocrSdkClient;
    }

    @PostMapping("/ocr/recognize")
    public ApiResponse<OcrRecognizeResponse> recognize(@Valid @RequestBody OcrRecognizeRequest req) {
        return ApiResponse.ok(ocrSdkClient.recognize(req.platform(), req.imageBase64()));
    }

    @PostMapping("/import")
    public ApiResponse<List<SourceItem>> importSources(@Valid @RequestBody SourceImportRequest req) {
        return ApiResponse.ok(store.importSources(req));
    }

    @PostMapping("/manual")
    public ApiResponse<SourceItem> manualAdd(@Valid @RequestBody ManualSourceRequest req) {
        return ApiResponse.ok(store.addSource(req.userId(), req.platform(), req.nickname(), req.homepage()));
    }

    @GetMapping
    public ApiResponse<List<SourceItem>> list(@RequestParam String userId) {
        return ApiResponse.ok(store.getSources(userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@RequestParam String userId, @PathVariable String id) {
        return ApiResponse.ok(store.removeSource(userId, id) ? "删除成功" : "未找到信息源");
    }
}
