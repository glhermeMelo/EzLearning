package com.ezlearning.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO que mapeia a resposta bruta da API externa de raciocínio.
 * Os campos podem vir com nomes diferentes dependendo do provedor;
 * usamos @JsonProperty se necessário ou mantemos os nomes padronizados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReasoningApiResponse(
    String answer,
    List<String> steps,
    Double confidence,
    String error
) {}
