package com.ezlearning.service;

import com.ezlearning.model.UploadedImage;
import com.ezlearning.repository.UploadedImageRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE);

    private final UploadedImageRepository repository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path imagesDir;
    private Path thumbnailsDir;

    public UploadServiceImpl(UploadedImageRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void initDirs() throws IOException {
        imagesDir = Paths.get(uploadDir, "images");
        thumbnailsDir = Paths.get(uploadDir, "thumbnails");
        Files.createDirectories(imagesDir);
        Files.createDirectories(thumbnailsDir);
    }

    @Override
    public UploadedImage upload(MultipartFile file, UUID userId) throws IOException {
        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Apenas imagens JPG e PNG são aceitas");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo excede o limite de 5MB");
        }

        var originalName = file.getOriginalFilename();
        var ext = extractExtension(originalName, contentType);
        var id = UUID.randomUUID();
        var storedFileName = id + "." + ext;

        var targetPath = imagesDir.resolve(storedFileName);
        try (var is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        var thumbnailFileName = id + "." + ext;
        var thumbnailPath = thumbnailsDir.resolve(thumbnailFileName);
        generateThumbnail(targetPath, thumbnailPath, ext);

        var image = new UploadedImage(
                originalName != null ? originalName : "unknown",
                "images/" + storedFileName,
                "thumbnails/" + thumbnailFileName,
                file.getSize(),
                contentType,
                userId
        );

        return repository.save(image);
    }

    @Override
    public UploadedImage getImage(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Imagem não encontrada: " + id));
    }

    @Override
    public byte[] loadOriginal(UUID id) throws IOException {
        var image = getImage(id);
        return Files.readAllBytes(Paths.get(uploadDir, image.getStoredPath()));
    }

    @Override
    public byte[] loadThumbnail(UUID id) throws IOException {
        var image = getImage(id);
        var thumbPath = image.getThumbnailPath();
        if (thumbPath == null) {
            return loadOriginal(id);
        }
        return Files.readAllBytes(Paths.get(uploadDir, thumbPath));
    }

    private void generateThumbnail(Path source, Path target, String ext) throws IOException {
        try (InputStream is = Files.newInputStream(source)) {
            var original = ImageIO.read(is);
            if (original == null) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            int width = 300;
            int height = (int) ((double) original.getHeight() / original.getWidth() * width);

            var thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            var g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(original, 0, 0, width, height, null);
            g2d.dispose();

            var format = "png".equalsIgnoreCase(ext) ? "png" : "jpg";
            ImageIO.write(thumbnail, format, target.toFile());
        }
    }

    private String extractExtension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            var ext = originalName.substring(originalName.lastIndexOf('.') + 1);
            if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) {
                return ext.toLowerCase();
            }
        }
        return MediaType.IMAGE_JPEG_VALUE.equals(contentType) ? "jpg" : "png";
    }
}
