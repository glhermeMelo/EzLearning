package com.ezlearning.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class MediaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MediaApiClient.class);

    private static final Map<String, Map<String, String>> TOPIC_PATTERNS = Map.of(
            "equacao.*segundo.*grau|equacao.*quadratica|bhaskara|2o grau",
            Map.of("title", "Equações do 2º Grau",
                    "concept1", "Identificar a, b, c",
                    "concept2", "Delta = b² - 4ac",
                    "concept3", "Delta >= 0?",
                    "result1", "Duas raízes reais",
                    "result2", "Sem raízes reais",
                    "formula", "x = (-b ± √Δ) / 2a"),

            "trigonometria|seno|cosseno|tangente|pitagoras",
            Map.of("title", "Trigonometria",
                    "concept1", "Triângulo Retângulo",
                    "concept2", "Catetos e Hipotenusa",
                    "concept3", "Ângulo conhecido?",
                    "result1", "Aplicar razões trigonométricas",
                    "result2", "Aplicar Teorema de Pitágoras",
                    "formula", "a² = b² + c²"),

            "funcao|grafico|dominio|imagem",
            Map.of("title", "Funções Matemáticas",
                    "concept1", "Domínio",
                    "concept2", "Contradomínio e Imagem",
                    "concept3", "É função?",
                    "result1", "Sim: cada x tem um único y",
                    "result2", "Não: relação não é função",
                    "formula", "f(x) = y")
    );

    private static final String DIAGRAM_TEMPLATE = """
            flowchart TD
                A["TITLE"] --> B[Passo 1: CONCEPT1]
                B --> C[Passo 2: CONCEPT2]
                C --> D["CONCEPT3"]
                D -->|Sim| E["RESULT1"]
                D -->|Não| F["RESULT2"]
                C --> G["FORMULA"]
                G --> H[Aplicar e Calcular Raízes]

                style A fill:#2196F3,stroke:#1976D2,color:#fff
                style B fill:#4CAF50,stroke:#388E3C,color:#fff
                style C fill:#4CAF50,stroke:#388E3C,color:#fff
                style D fill:#FF9800,stroke:#F57C00,color:#fff
                style E fill:#9C27B0,stroke:#7B1FA2,color:#fff
                style F fill:#f44336,stroke:#D32F2F,color:#fff
                style G fill:#FFC107,stroke:#FFA000,color:#000
                style H fill:#2196F3,stroke:#1976D2,color:#fff
                """;

    private static final String MINIMAL_TEMPLATE = """
            flowchart TD
                A["TITLE"] --> B[Passo 1: CONCEPT1]
                B --> C[Passo 2: CONCEPT2]
                C --> D["Aplicar FORMULA"]
                D --> E["Resultado"]

                style A fill:#2196F3,stroke:#1976D2,color:#fff
                style B fill:#4CAF50,stroke:#388E3C,color:#fff
                style C fill:#4CAF50,stroke:#388E3C,color:#fff
                style D fill:#FF9800,stroke:#F57C00,color:#fff
                style E fill:#9C27B0,stroke:#7B1FA2,color:#fff
                """;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String generateDiagram(String prompt) {
        String template = selectTemplate(prompt);
        Map<String, String> terms = extractTerms(prompt);
        String filled = fillTemplate(template, terms);
        log.debug("Generated diagram template for prompt ({} chars)", filled.length());
        return filled;
    }

    private String selectTemplate(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("explic") || lower.contains("passo") || lower.contains("como resolver")
                || lower.contains("formula") || lower.contains("steps") || lower.contains("how to")) {
            return DIAGRAM_TEMPLATE;
        }
        return MINIMAL_TEMPLATE;
    }

    private Map<String, String> extractTerms(String prompt) {
        String lower = prompt.toLowerCase();

        for (var entry : TOPIC_PATTERNS.entrySet()) {
            if (Pattern.compile(entry.getKey()).matcher(lower).find()) {
                log.debug("Matched topic pattern: {}", entry.getKey());
                return entry.getValue();
            }
        }

        String title = extractTitle(prompt);
        return Map.of(
                "title", title,
                "concept1", "Identificar os elementos",
                "concept2", "Calcular os valores",
                "concept3", "Verificar as condições",
                "result1", "Solução encontrada",
                "result2", "Revisar os dados",
                "formula", "Expressão matemática"
        );
    }

    private String extractTitle(String prompt) {
        String cleaned = prompt.replaceAll("(?i)(diagrama|fluxograma|mapa mental|mindmap|flowchart|gerar|criar|desenhar)\\s*(de |do |da |sobre |um |uma )?", "");
        cleaned = cleaned.replaceAll("(?i)(explicando|explicar|mostrando|mostrar|passo a passo|step by step)\\s*", "");
        cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.length() > 60) cleaned = cleaned.substring(0, 57) + "...";
        if (cleaned.isEmpty()) cleaned = "Diagrama Educacional";
        String[] words = cleaned.split(" ");
        StringBuilder title = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                title.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) title.append(w.substring(1));
                title.append(" ");
            }
        }
        return title.toString().trim();
    }

    private String fillTemplate(String template, Map<String, String> terms) {
        String result = template;
        for (var entry : terms.entrySet()) {
            result = result.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        return result;
    }
}
