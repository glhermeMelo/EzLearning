package com.ezlearning.service;

import com.ezlearning.integration.TtsApiClient;
import com.ezlearning.model.dto.TtsResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class TtsServiceImpl implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsServiceImpl.class);

    private static final String AUDIO_SUBDIR = "audio";

    private final TtsApiClient ttsApiClient;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path audioDir;

    public TtsServiceImpl(TtsApiClient ttsApiClient) {
        this.ttsApiClient = ttsApiClient;
    }

    @PostConstruct
    void initDirs() throws IOException {
        audioDir = Paths.get(uploadDir, AUDIO_SUBDIR);
        Files.createDirectories(audioDir);
    }

    @Override
    public TtsResponse synthesize(String text, String voice) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio");
        }

        String effectiveVoice = voice != null ? voice : "pt";

        byte[] audio = ttsApiClient.synthesize(text, effectiveVoice);

        var id = UUID.randomUUID();
        var fileName = id + ".mp3";
        var targetPath = audioDir.resolve(fileName);

        try {
            Files.write(targetPath, audio);
            log.debug("Saved audio file: {} ({} bytes)", fileName, audio.length);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo de áudio: " + e.getMessage(), e);
        }

        return new TtsResponse("/api/tts/" + id, 0.0, "mp3");
    }

    @Override
    public void synthesizeAndStream(String text, String voice, OutputStream out) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("O texto não pode estar vazio");
        }

        String effectiveVoice = voice != null ? voice : "pt";

        byte[] audio = ttsApiClient.synthesize(text, effectiveVoice);

        try {
            out.write(audio);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao transmitir áudio: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] loadAudio(String id) {
        var targetPath = audioDir.resolve(id + ".mp3");
        if (!Files.exists(targetPath)) {
            throw new IllegalArgumentException("Áudio não encontrado: " + id);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo de áudio: " + e.getMessage(), e);
        }
    }
}
