package com.ezlearning.integration;

import com.ezlearning.model.dto.MediaGenerationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class MediaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MediaApiClient.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public MediaApiClient(
            @Qualifier("mediaRestTemplate") RestTemplate restTemplate,
            @Value("${app.api.media.url}") String apiUrl,
            @Value("${app.api.media.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Chama a API externa de geração de mídia com o prompt fornecido.
     *
     * @param request o pedido de geração contendo prompt e opções
     * @return mapa com a resposta da API (contendo "url" ou "base64")
     */
    public Map<String, Object> generateImage(MediaGenerationRequest request) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        var body = Map.of(
            "prompt", request.prompt(),
            "style", request.style() != null ? request.style() : "default",
            "diagram_type", request.diagramType() != null ? request.diagramType() : "",
            "options", request.options() != null ? request.options() : Map.of()
        );

        var entity = new HttpEntity<>(body, headers);

        log.debug("Calling media API at {} with prompt: {}", apiUrl, request.prompt());

        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl + "/generate",
            HttpMethod.POST,
            entity,
            Map.class
        );

        return response.getBody();
    }
}
