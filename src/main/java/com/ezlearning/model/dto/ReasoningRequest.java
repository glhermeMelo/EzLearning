package com.ezlearning.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasoningRequest(
    @NotBlank String question,
    String context
) {}
