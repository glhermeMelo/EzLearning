package com.ezlearning.model.dto;

import java.util.UUID;

public record MediaGenerationResponse(
    UUID id,
    String url,
    String thumbnailUrl,
    String originalName,
    long size,
    String mimeType
) {}
