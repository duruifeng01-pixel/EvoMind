package com.evomind.api.controller;

import com.evomind.api.model.*;
import com.evomind.api.store.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CognitionController {

    private final InMemoryStore store;

    public CognitionController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/feed")
    public ApiResponse<List<CardItem>> feed(@RequestParam String userId) {
        return ApiResponse.ok(store.feed(userId));
    }

    @GetMapping("/{id}/mindmap")
    public ApiResponse<MindmapResponse> mindmap(@PathVariable String id) {
        return ApiResponse.ok(store.mindmap(id));
    }

    @GetMapping("/{id}/drilldown")
    public ApiResponse<DrilldownResponse> drilldown(@PathVariable String id, @RequestParam String nodeId) {
        return ApiResponse.ok(store.drilldown(id, nodeId));
    }
}
