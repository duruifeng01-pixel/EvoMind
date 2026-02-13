package com.evomind.api.model;

import java.util.List;

public record MindmapResponse(String cardId, String root, List<Node> nodes, String tag) {
    public record Node(String id, String text, String level, boolean conflict) {}
}
