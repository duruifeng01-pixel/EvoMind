package com.evomind.api.model;

public record SourceItem(
        String id,
        String platform,
        String nickname,
        String homepage,
        boolean pinned,
        String groupName,
        String createdAt
) {}
