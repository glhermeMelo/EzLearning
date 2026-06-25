package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MediaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MediaApiClient.class);

    private static final String MODEL = "qwen2.5-coder:3b";

    private static final String SYSTEM_PROMPT = """
            You are a diagram generator. Given a description, output ONLY valid Mermaid diagram code.
            Rules:
            - Output raw Mermaid code only. No markdown fences, no backticks, no explanations.
            - Use valid Mermaid syntax (graph, flowchart, sequenceDiagram, classDiagram, etc).
            - Keep node labels short and clear.
            - Do not include any text before or after the diagram code.
            """;

    private final RestClient restClient;

    public MediaApiClient(
            @Qualifier("reasoningRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
    }

    public String generateDiagram(String prompt) {
        var chatRequest = new ChatRequest(
                MODEL,
                List.of(
                        new Message("system", SYSTEM_PROMPT),
                        new Message("user", prompt)
                )
        );

        log.debug("Sending diagram request to Ollama ({})", MODEL);

        var chatResponse = restClient.post()
                .uri("")
                .body(chatRequest)
                .retrieve()
                .body(ChatResponse.class);

        String mermaid = extractText(chatResponse);
        mermaid = cleanMermaid(mermaid);
        log.debug("Received Mermaid code from Ollama ({} chars)", mermaid.length());
        return mermaid;
    }

    private String cleanMermaid(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Resposta vazia do Ollama");
        }
        String text = raw.strip();

        // Se houver bloco cercado ```mermaid ... ```, extrai o conteudo dele
        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = text.indexOf('\n', fenceStart);
            int fenceEnd = text.indexOf("```", fenceStart + 3);
            if (contentStart > 0 && fenceEnd > contentStart) {
                text = text.substring(contentStart + 1, fenceEnd).strip();
            }
        }

        // Palavras-chave que iniciam um diagrama Mermaid valido
        String[] starters = {
                "graph ", "graph\n", "flowchart ", "sequenceDiagram",
                "classDiagram", "stateDiagram", "erDiagram", "journey",
                "gantt", "pie", "mindmap", "timeline"
        };

        int diagramStart = -1;
        for (String s : starters) {
            int idx = text.indexOf(s);
            if (idx >= 0 && (diagramStart < 0 || idx < diagramStart)) {
                diagramStart = idx;
            }
        }
        if (diagramStart > 0) {
            text = text.substring(diagramStart);
        }

        // Corta linhas finais que claramente nao sao diagrama (texto explicativo)
        var lines = text.split("\n");
        var sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.startsWith("**") || trimmed.startsWith("##")
                    || trimmed.toLowerCase().startsWith("explanation")
                    || trimmed.toLowerCase().startsWith("this ")
                    || trimmed.startsWith("```")) {
                break;
            }
            sb.append(line).append("\n");
        }

        String cleaned = sb.toString().strip();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Código Mermaid vazio após limpeza");
        }
        return cleaned;
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

    public record ChatRequest(String model, List<Message> messages) {}

    public record Message(String role, String content) {}

    public record ChatResponse(List<Choice> choices) {
        public record Choice(Message message) {}
    }
}
