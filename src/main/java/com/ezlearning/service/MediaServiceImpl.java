package com.ezlearning.service;

import com.ezlearning.integration.MediaApiClient;
import com.ezlearning.model.GeneratedMedia;
import com.ezlearning.model.dto.MediaGenerationRequest;
import com.ezlearning.model.dto.MediaGenerationResponse;
import com.ezlearning.repository.GeneratedMediaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
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

        // 1. Verificar cache no Redis
        var cachedId = redisTemplate.opsForValue().get(CACHE_PREFIX + promptHash);
        if (cachedId != null) {
            try {
                var uuid = UUID.fromString(cachedId);
                var existing = repository.findById(uuid);
                if (existing.isPresent()) {
                    var media = existing.get();
                    log.debug("Cache hit for prompt hash: {}", promptHash);
                    return buildResponse(media);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid cached UUID for hash {}: {}", promptHash, cachedId);
            }
        }

        // 2. Verificar cache no banco (fallback)
        var existingByHash = repository.findByPromptHash(promptHash);
        if (existingByHash.isPresent()) {
            var media = existingByHash.get();
            log.debug("Database cache hit for prompt hash: {}", promptHash);
            redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);
            return buildResponse(media);
        }

        // 3. Chamar API externa
        Map<String, Object> apiResponse;
        try {
            apiResponse = mediaApiClient.generateImage(request);
        } catch (Exception e) {
            log.error("Failed to call media API for prompt: {}", request.prompt(), e);
            throw new IllegalArgumentException("Falha ao gerar imagem: serviço de mídia indisponível");
        }

        if (apiResponse == null || apiResponse.isEmpty()) {
            throw new IllegalArgumentException("Falha ao gerar imagem: resposta vazia da API");
        }

        // 4. Processar resposta (URL ou base64)
        GeneratedMedia media = processApiResponse(apiResponse, request.prompt(), promptHash, userId);

        // 5. Cache no Redis
        redisTemplate.opsForValue().set(CACHE_PREFIX + promptHash, media.getId().toString(), CACHE_TTL);

        return buildResponse(media);
    }

    @Override
    public byte[] loadMedia(UUID id) throws IOException {
        var media = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mídia não encontrada: " + id));

        // Atualizar último acesso
        media.setLastAccessedAt(LocalDateTime.now());
        repository.save(media);

        return Files.readAllBytes(Paths.get(uploadDir, media.getStoredPath()));
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

                // Remover do cache Redis
                redisTemplate.delete(CACHE_PREFIX + media.getPromptHash());

                repository.delete(media);
                log.debug("Deleted unreferenced media: {} (prompt: {})", media.getId(), media.getPrompt());
            } catch (IOException e) {
                log.error("Failed to delete media file: {}", media.getStoredPath(), e);
            }
        }
    }

    // ========== Métodos internos ==========

    private GeneratedMedia processApiResponse(Map<String, Object> response,
                                               String prompt,
                                               String promptHash,
                                               UUID userId) {
        var id = UUID.randomUUID();
        var fileName = id + ".png";
        var storedPath = DIAGRAMS_SUBDIR + "/" + fileName;
        var targetPath = diagramsDir.resolve(fileName);

        try {
            // Verificar se a resposta contém URL
            var urlObj = response.get("url");
            if (urlObj instanceof String url && !url.isBlank()) {
                downloadImage(url, targetPath);
                var size = Files.size(targetPath);
                return repository.save(new GeneratedMedia(prompt, promptHash, storedPath, "image/png", size, userId));
            }

            // Verificar se a resposta contém base64
            var base64Obj = response.get("base64");
            if (base64Obj instanceof String base64 && !base64.isBlank()) {
                var imageBytes = decodeBase64(base64);
                Files.write(targetPath, imageBytes);
                var size = Files.size(targetPath);
                return repository.save(new GeneratedMedia(prompt, promptHash, storedPath, "image/png", size, userId));
            }

            // Verificar se a resposta contém data (outro formato comum)
            var dataObj = response.get("data");
            if (dataObj instanceof String data && !data.isBlank()) {
                var imageBytes = decodeBase64(data);
                Files.write(targetPath, imageBytes);
                var size = Files.size(targetPath);
                return repository.save(new GeneratedMedia(prompt, promptHash, storedPath, "image/png", size, userId));
            }

            throw new IllegalArgumentException("Formato de resposta não suportado. Esperado 'url' ou 'base64'.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao processar imagem gerada: " + e.getMessage());
        }
    }

    private void downloadImage(String imageUrl, Path targetPath) throws IOException {
        try {
            var response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            if (response.getStatusCode().isError()) {
                throw new IOException("Falha ao baixar imagem, status: " + response.getStatusCode());
            }

            var body = response.getBody();
            if (body == null) {
                throw new IOException("Resposta vazia ao baixar imagem");
            }

            Files.write(targetPath, body);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Erro ao baixar imagem de " + imageUrl, e);
        }
    }

    private byte[] decodeBase64(String base64) {
        try {
            // Remover prefixo data:image if present
            String data = base64;
            if (data.contains(",")) {
                data = data.substring(data.indexOf(',') + 1);
            }
            return Base64.getDecoder().decode(data.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Base64 inválido na resposta da API");
        }
    }

    private MediaGenerationResponse buildResponse(GeneratedMedia media) {
        return new MediaGenerationResponse(
                media.getId(),
                "/api/media/" + media.getId(),
                null,
                media.getPrompt().length() > 50
                        ? media.getPrompt().substring(0, 50) + "..."
                        : media.getPrompt(),
                media.getSize(),
                media.getMimeType()
        );
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
