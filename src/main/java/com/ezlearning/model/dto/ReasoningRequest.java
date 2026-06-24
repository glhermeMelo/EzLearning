package com.ezlearning.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasoningRequest(
    @NotBlank @Size(max = 2000) String question,
    String context
) {}
