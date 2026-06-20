package com.ezlearning.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record MediaGenerationRequest(
    @NotBlank String prompt,
    String style,
    String diagramType,
    Map<String, String> options
) {}
