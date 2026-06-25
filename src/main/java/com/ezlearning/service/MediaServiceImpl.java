package com.ezlearning.service;

import com.ezlearning.integration.MediaApiClient;
import com.ezlearning.model.GeneratedMedia;
import com.ezlearning.model.dto.MediaGenerationRequest;
import com.ezlearning.model.dto.MediaGenerationResponse;
import com.ezlearning.model.dto.MediaRequest;
import com.ezlearning.model.dto.MediaResponse;
import com.ezlearning.repository.GeneratedMediaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    private static final String CACHE_PREFIX = "media:prompt:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final long CLEANUP_AGE_HOURS = 1;
    private static final String DIAGRAMS_SUBDIR = "diagrams";

    private final MediaApiClient mediaApiClient;
    private final GeneratedMediaRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.kroki.url:http://kroki:8000}")
    private String krokiUrl;

    private Path diagramsDir;

    public MediaServiceImpl(MediaApiClient mediaApiClient,
                            GeneratedMediaRepository repository,
                            StringRedisTemplate redisTemplate,
                            @Qualifier("mediaRestTemplate") RestTemplate restTemplate) {
        this.mediaApiClient = mediaApiClient;
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void initDirs() throws IOException {
        diagramsDir = Paths.get(uploadDir, DIAGRAMS_SUBDIR);
        Files.createDirectories(diagramsDir);
    }

    @Override
    public MediaGenerationResponse generateMedia(MediaGenerationRequest request, UUID userId) {
        var promptHash = hashPrompt(request.prompt());

        var cachedId = redisTemplate.opsForValue().get(CACHE_PREFIX + promptHash);
        if (cachedId != null) {
            try {
                var uuid = UUID.fromString(cachedId);
                var existing = repository.findById(uuid);
                if (existing.isPresent()) {
                    log.debug("Cache hit for prompt hash: {}", promptHash);
                    return buildResponse(existing.get());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cached UUID for hash {}: {}", promptHash, cachedId);
            }
        }

        var existingByHash = repository.findByPromptHash(promptHash);
        if (existingByHash.isPresent()) {
            var media = existingByHash.get();
            log.debug("Database cache hit for prompt hash: {}", promptHash);
            redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);
            return buildResponse(media);
        }

        var enhancedPrompt = buildEnhancedPrompt(request);
        GeneratedMedia media = createDiagram(enhancedPrompt, request.prompt(), promptHash, userId);

        redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);
        return buildResponse(media);
    }

    @Override
    public MediaResponse generateDiagram(MediaRequest request) {
        var promptHash = hashPrompt(request.prompt());

        var cachedId = redisTemplate.opsForValue().get(CACHE_PREFIX + promptHash);
        if (cachedId != null) {
            try {
                var uuid = UUID.fromString(cachedId);
                var existing = repository.findById(uuid);
                if (existing.isPresent()) {
                    log.debug("Cache hit for diagram prompt hash: {}", promptHash);
                    return buildDiagramResponse(existing.get());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cached UUID for hash {}: {}", promptHash, cachedId);
            }
        }

        var existingByHash = repository.findByPromptHash(promptHash);
        if (existingByHash.isPresent()) {
            var media = existingByHash.get();
            log.debug("Database cache hit for diagram prompt hash: {}", promptHash);
            redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);
            return buildDiagramResponse(media);
        }

        GeneratedMedia media = createDiagram(request.prompt(), request.prompt(), promptHash, null);

        redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);
        return buildDiagramResponse(media);
    }

    @Override
    public byte[] loadMedia(UUID id) throws IOException {
        var media = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mídia não encontrada: " + id));

        media.setLastAccessedAt(LocalDateTime.now());
        repository.save(media);

        return Files.readAllBytes(Paths.get(uploadDir, media.getStoredPath()));
    }

    @Override
    public String getMimeType(UUID id) {
        return repository.findById(id)
                .map(GeneratedMedia::getMimeType)
                .orElse("application/octet-stream");
    }

    @Override
    @Scheduled(fixedRateString = "${app.media.cleanup.rate:3600000}")
    public void cleanupUnreferencedMedia() {
        var threshold = LocalDateTime.now().minusHours(CLEANUP_AGE_HOURS);
        var oldMedias = repository.findByReferencedFalseAndCreatedAtBefore(threshold);

        if (oldMedias.isEmpty()) {
            log.debug("No unreferenced media to clean up");
            return;
        }

        log.info("Cleaning up {} unreferenced media files older than {} hours", oldMedias.size(), CLEANUP_AGE_HOURS);

        for (var media : oldMedias) {
            try {
                var filePath = Paths.get(uploadDir, media.getStoredPath());
                Files.deleteIfExists(filePath);

                var imagePath = Paths.get(uploadDir, DIAGRAMS_SUBDIR, media.getId() + ".png");
                Files.deleteIfExists(imagePath);

                redisTemplate.delete(CACHE_PREFIX + media.getPromptHash());

                repository.delete(media);
                log.debug("Deleted unreferenced media: {} (prompt: {})", media.getId(), media.getPrompt());
            } catch (IOException e) {
                log.error("Failed to delete media file: {}", media.getStoredPath(), e);
            }
        }
    }

    private GeneratedMedia createDiagram(String enhancedPrompt, String originalPrompt, String promptHash, UUID userId) {
        String mermaid;
        try {
            mermaid = mediaApiClient.generateDiagram(enhancedPrompt);
        } catch (Exception e) {
            log.error("Failed to generate Mermaid code for prompt: {}", originalPrompt, e);
            throw new IllegalArgumentException("Falha ao gerar diagrama: serviço de mídia indisponível");
        }

        byte[] png;
        try {
            png = renderMermaid(mermaid);
        } catch (Exception e) {
            log.error("Failed to render Mermaid via Kroki for prompt: {}", originalPrompt, e);
            throw new IllegalArgumentException("Falha ao renderizar diagrama");
        }

        var id = UUID.randomUUID();
        var mmdName = id + ".mmd";
        var pngName = id + ".png";
        var storedPath = DIAGRAMS_SUBDIR + "/" + mmdName;

        try {
            // Salva o codigo Mermaid (fonte) e o PNG renderizado
            Files.writeString(diagramsDir.resolve(mmdName), mermaid, StandardCharsets.UTF_8);
            Files.write(diagramsDir.resolve(pngName), png);
            var size = Files.size(diagramsDir.resolve(mmdName));

            return repository.save(new GeneratedMedia(originalPrompt, promptHash, storedPath, "text/vnd.mermaid", size, userId));
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao processar diagrama gerado: " + e.getMessage());
        }
    }

    private byte[] renderMermaid(String mermaidCode) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var body = Map.of("diagram_source", mermaidCode);
        var entity = new HttpEntity<>(body, headers);

        var response = restTemplate.exchange(
                krokiUrl + "/mermaid/png",
                HttpMethod.POST,
                entity,
                byte[].class
        );

        if (response.getStatusCode().isError() || response.getBody() == null) {
            throw new IllegalStateException("Kroki retornou erro: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String buildEnhancedPrompt(MediaGenerationRequest request) {
        var sb = new StringBuilder(request.prompt());
        if (request.diagramType() != null && !request.diagramType().isBlank()) {
            sb.append("\n\nDiagram type: ").append(request.diagramType());
        }
        if (request.style() != null && !request.style().isBlank()) {
            sb.append("\nStyle: ").append(request.style());
        }
        return sb.toString();
    }

    private MediaGenerationResponse buildResponse(GeneratedMedia media) {
        return new MediaGenerationResponse(
                media.getId(),
                "/api/media/" + media.getId(),
                "/api/media/" + media.getId() + "/image",
                media.getPrompt().length() > 50
                        ? media.getPrompt().substring(0, 50) + "..."
                        : media.getPrompt(),
                media.getSize(),
                media.getMimeType()
        );
    }

    private MediaResponse buildDiagramResponse(GeneratedMedia media) {
        var imagePath = diagramsDir.resolve(media.getId() + ".png");
        var imageUrl = Files.exists(imagePath) ? "/api/media/" + media.getId() + "/image" : null;
        return new MediaResponse(media.getId(), "/api/media/" + media.getId(), imageUrl);
    }

    static String hashPrompt(String prompt) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
    }
}
