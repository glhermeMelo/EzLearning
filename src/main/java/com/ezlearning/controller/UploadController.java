package com.ezlearning.controller;

import com.ezlearning.model.dto.UploadResponse;
import com.ezlearning.service.UploadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        var image = uploadService.upload(file, null);
        var response = new UploadResponse(
                image.getId(),
                "/api/uploads/" + image.getId(),
                "/api/uploads/" + image.getId() + "/thumbnail",
                image.getOriginalName(),
                image.getSize()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serveOriginal(@PathVariable UUID id) throws IOException {
        var image = uploadService.getImage(id);
        var data = uploadService.loadOriginal(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .body(data);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> serveThumbnail(@PathVariable UUID id) throws IOException {
        var image = uploadService.getImage(id);
        var data = uploadService.loadThumbnail(id);
        var mimeType = image.getMimeType() != null ? image.getMimeType() : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumb_" + image.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(data);
    }
}
