package com.ezlearning.model.dto;

import java.util.UUID;

public record MediaResponse(
    UUID id,
    String description,
    String imageUrl
) {}
