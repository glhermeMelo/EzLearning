package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ReasoningApiClient {

    private static final Logger log = LoggerFactory.getLogger(ReasoningApiClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public ReasoningApiClient(
            @Qualifier("reasoningRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
        this.apiKey = properties.reasoning().key();
    }

    public ReasoningResponse ask(ReasoningRequest request) {
        String prompt = request.context() != null && !request.context().isBlank()
                ? request.context() + "\n\n" + request.question()
                : request.question();

        var geminiRequest = new GeminiRequest(List.of(
                new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))
        ));

        log.debug("Sending request to Gemini API");

        var geminiResponse = restClient.post()
                .uri("?key={key}", apiKey)
                .body(geminiRequest)
                .retrieve()
                .body(GeminiResponse.class);

        String markdown = extractText(geminiResponse);

        log.debug("Received response from Gemini API ({} chars)", markdown.length());

        return new ReasoningResponse(markdown, List.of(), 0.0);
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalArgumentException("Resposta vazia da API Gemini");
        }
        var parts = response.candidates().getFirst().content().parts();
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Resposta sem conteúdo da API Gemini");
        }
        return parts.getFirst().text();
    }

    public record GeminiRequest(List<Content> contents) {
        public record Content(List<Part> parts) {}
        public record Part(String text) {}
    }

    public record GeminiResponse(List<Candidate> candidates) {
        public record Candidate(Content content) {}
        public record Content(List<Part> parts) {}
        public record Part(String text) {}
    }
}
