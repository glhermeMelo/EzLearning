package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class MediaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MediaApiClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public MediaApiClient(
            @Qualifier("mediaRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
        this.apiKey = properties.media().key();
    }

    public String generateDiagram(String prompt) {
        var geminiRequest = new GeminiRequest(List.of(
                new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))
        ));

        log.debug("Sending diagram generation request to Gemini API");

        URI uri = UriComponentsBuilder.fromUriString("?key={key}").build(apiKey);
        var geminiResponse = restClient.post()
                .uri(uri)
                .body(geminiRequest)
                .retrieve()
                .body(GeminiResponse.class);

        String markdown = extractText(geminiResponse);

        log.debug("Received diagram response from Gemini API ({} chars)", markdown.length());

        return markdown;
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
