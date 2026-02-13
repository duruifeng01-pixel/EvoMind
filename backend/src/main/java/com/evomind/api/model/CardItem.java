package com.evomind.api.model;

public record CardItem(
        String id,
        String source,
        String platform,
        String title,
        String guide,
        boolean aiGenerated
) {}
