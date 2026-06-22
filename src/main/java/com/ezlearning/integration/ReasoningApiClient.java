package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import com.ezlearning.model.dto.ReasoningApiResponse;
import com.ezlearning.model.dto.ReasoningRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ReasoningApiClient {

    private static final Logger log = LoggerFactory.getLogger(ReasoningApiClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public ReasoningApiClient(
            @Qualifier("reasoningRestTemplate") RestTemplate restTemplate,
            AiApiProperties properties) {
        this.restTemplate = restTemplate;
        this.apiUrl = properties.url();
        this.apiKey = properties.key();
    }

    /**
     * Envia uma pergunta para a API de raciocínio externa, com retry e backoff.
     *
     * @param request a pergunta e contexto opcional
     * @return resposta estruturada da API
     * @throws IllegalArgumentException se a API retornar erro ou exceder tentativas
     */
    public ReasoningApiResponse sendReasoningRequest(ReasoningRequest request) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        var body = Map.of(
            "question", request.question(),
            "context", request.context() != null ? request.context() : ""
        );

        var entity = new HttpEntity<>(body, headers);

        String url = apiUrl + (apiUrl.endsWith("/") ? "reason" : "/reason");

        log.debug("Calling reasoning API at {}", apiUrl);

        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<ReasoningApiResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        ReasoningApiResponse.class
                );

                var apiResponse = response.getBody();

                if (apiResponse == null) {
                    throw new IllegalArgumentException("Resposta vazia da API de raciocínio");
                }

                if (apiResponse.error() != null && !apiResponse.error().isBlank()) {
                    throw new IllegalArgumentException("Erro da API de raciocínio: " + apiResponse.error());
                }

                log.debug("Reasoning API call succeeded on attempt {}", attempt);
                return apiResponse;

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < MAX_RETRIES) {
                    log.warn("Rate limited by reasoning API (attempt {}/{})", attempt, MAX_RETRIES);
                    lastException = e;
                    backoff(attempt);
                    continue;
                }
                if (e.getStatusCode().is4xxClientError()) {
                    var message = String.format("Erro %d na API de raciocínio: %s",
                            e.getStatusCode().value(), e.getResponseBodyAsString());
                    log.error(message);
                    throw new IllegalArgumentException(message);
                }
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    backoff(attempt);
                }

            } catch (HttpServerErrorException e) {
                log.warn("Server error from reasoning API (attempt {}/{}): {}",
                        attempt, MAX_RETRIES, e.getStatusCode());
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    backoff(attempt);
                }

            } catch (ResourceAccessException e) {
                log.warn("Timeout connecting to reasoning API (attempt {}/{}): {}",
                        attempt, MAX_RETRIES, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    backoff(attempt);
                }
            }
        }

        throw new IllegalArgumentException(
                "Falha ao comunicar com API de raciocínio após " + MAX_RETRIES + " tentativas: "
                        + lastException.getMessage());
    }

    private void backoff(int attempt) {
        long delay = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Requisição interrompida durante backoff", ie);
        }
    }
}
