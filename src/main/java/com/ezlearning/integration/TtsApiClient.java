package com.ezlearning.integration;

import com.ezlearning.config.AiApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TtsApiClient {

    private static final Logger log = LoggerFactory.getLogger(TtsApiClient.class);

    private final RestClient restClient;
    private final String ttsUrl;

    public TtsApiClient(
            @Qualifier("ttsRestClient") RestClient restClient,
            AiApiProperties properties) {
        this.restClient = restClient;
        this.ttsUrl = properties.tts().url();
    }

    public byte[] synthesize(String text, String voice) {
        var request = new KokoroTtsRequest("kokoro", text, voice);

        log.debug("Sending TTS request to Kokoro API, text length: {} chars, voice: {}", text.length(), voice);

        byte[] audio = restClient.post()
                .uri(ttsUrl)
                .body(request)
                .retrieve()
                .body(byte[].class);

        if (audio == null || audio.length == 0) {
            throw new RuntimeException("Resposta vazia da API de TTS");
        }

        log.debug("Received audio response from Kokoro API, size: {} bytes", audio.length);

        return audio;
    }

    public record KokoroTtsRequest(String model, String input, String voice) {}
}
