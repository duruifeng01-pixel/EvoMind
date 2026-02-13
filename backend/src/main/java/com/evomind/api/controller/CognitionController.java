package com.evomind.api.controller;

import com.evomind.api.integration.AiSdkClient;
import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CognitionController {

    private final InMemoryStore store;
    private final AiSdkClient aiSdkClient;

    public CognitionController(InMemoryStore store, AiSdkClient aiSdkClient) {
        this.store = store;
        this.aiSdkClient = aiSdkClient;
    }

    @GetMapping("/feed")
    public ApiResponse<List<CardItem>> feed(@RequestParam String userId) {
        // 演示层：优先用AI客户端产出，再可融合store缓存
        return ApiResponse.ok(aiSdkClient.buildCards(userId));
    }

    @GetMapping("/{id}/mindmap")
    public ApiResponse<MindmapResponse> mindmap(@PathVariable String id) {
        return ApiResponse.ok(aiSdkClient.buildMindmap(id));
    }

    @GetMapping("/{id}/drilldown")
    public ApiResponse<DrilldownResponse> drilldown(@PathVariable String id, @RequestParam String nodeId) {
        return ApiResponse.ok(store.drilldown(id, nodeId));
    }
}
