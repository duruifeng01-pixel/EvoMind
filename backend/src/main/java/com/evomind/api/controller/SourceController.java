package com.evomind.api.controller;

import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    private final InMemoryStore store;

    public SourceController(InMemoryStore store) {
        this.store = store;
    }

    @PostMapping("/ocr/recognize")
    public ApiResponse<OcrRecognizeResponse> recognize(@Valid @RequestBody OcrRecognizeRequest req) {
        List<OcrRecognizeResponse.SourceCandidate> candidates = List.of(
                new OcrRecognizeResponse.SourceCandidate("科技博主A", "https://example.cn/a"),
                new OcrRecognizeResponse.SourceCandidate("产品观察B", "https://example.cn/b")
        );
        return ApiResponse.ok(new OcrRecognizeResponse(800, candidates, "AI生成，仅供参考"));
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
