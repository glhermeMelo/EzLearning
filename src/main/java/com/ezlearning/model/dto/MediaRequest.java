package com.ezlearning.model.dto;

import jakarta.validation.constraints.NotBlank;

public record MediaRequest(
    @NotBlank String prompt
) {}
