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

    private static final String MODEL = "gemma2:2b";

    private final RestClient restClient;

    public ReasoningApiClient(
            @Qualifier("reasoningRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
    }

    public ReasoningResponse ask(ReasoningRequest request) {
        String prompt = request.context() != null && !request.context().isBlank()
                ? request.context() + "\n\n" + request.question()
                : request.question();

        var chatRequest = new ChatRequest(
                MODEL,
                List.of(new Message("user", prompt)),
                false
        );

        log.debug("Sending request to Ollama ({})", MODEL);

        try {
            var chatResponse = restClient.post()
                    .uri("")
                    .body(chatRequest)
                    .retrieve()
                    .body(ChatResponse.class);

            String markdown = extractText(chatResponse);
            log.debug("Received response from Ollama ({} chars)", markdown.length());
            return new ReasoningResponse(markdown, List.of(), 0.0);
        } catch (Exception e) {
            log.error("Failed to get response from Ollama: {}", e.getMessage());
            return new ReasoningResponse("Erro ao consultar a IA: " + e.getMessage(), List.of(), 0.0);
        }
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("Resposta vazia do Ollama");
        }
        var message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new IllegalArgumentException("Resposta sem conteúdo do Ollama");
        }
        return message.content();
    }

    // ---- Request (formato OpenAI / Ollama) ----
    public record ChatRequest(String model, List<Message> messages, boolean stream) {}

    public record Message(String role, String content) {}

    // ---- Response (formato OpenAI / Ollama) ----
    public record ChatResponse(List<Choice> choices) {
        public record Choice(Message message) {}
    }
}
