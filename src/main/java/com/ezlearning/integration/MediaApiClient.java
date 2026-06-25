package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class MediaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MediaApiClient.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;

    public MediaApiClient(
            @Qualifier("mediaRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
        this.apiUrl = properties.media().url();
        this.apiKey = properties.media().key();
    }

    public String generateDiagram(String prompt) {
        var geminiRequest = new GeminiRequest(List.of(
                new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))
        ));

        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("key", apiKey)
                .build()
                .toUri();

        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.debug("Sending diagram generation request to Gemini API (attempt {}/{})", attempt, MAX_ATTEMPTS);

                var geminiResponse = restClient.post()
                        .uri(uri)
                        .body(geminiRequest)
                        .retrieve()
                        .body(GeminiResponse.class);

                String markdown = extractText(geminiResponse);
                log.debug("Received diagram response from Gemini API ({} chars)", markdown.length());
                return markdown;

            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx e timeout/conexao: transitorios, vale retry
                lastError = e;
                log.warn("Transient error on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            } catch (HttpClientErrorException.TooManyRequests e) {
                // 429: rate limit, vale retry
                lastError = e;
                log.warn("Rate limited (429) on attempt {}/{}", attempt, MAX_ATTEMPTS);
            } catch (HttpClientErrorException e) {
                // demais 4xx: erro do request, nao adianta repetir
                log.error("Client error from Gemini API, not retrying: {}", e.getStatusCode());
                throw e;
            }

            if (attempt < MAX_ATTEMPTS) {
                sleep(BASE_BACKOFF_MS * (long) Math.pow(2, attempt - 1)); // 1s, 2s, 4s
            }
        }

        log.error("All {} attempts failed for diagram generation", MAX_ATTEMPTS);
        throw lastError;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrompido", ie);
        }
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
