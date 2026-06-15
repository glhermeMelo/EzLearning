package com.ezlearning.service;

import com.ezlearning.model.UploadedImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface UploadService {
    UploadedImage upload(MultipartFile file, UUID userId) throws IOException;
    UploadedImage getImage(UUID id);
    byte[] loadOriginal(UUID id) throws IOException;
    byte[] loadThumbnail(UUID id) throws IOException;
}
