package com.ezlearning.repository;

import com.ezlearning.model.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, UUID> {
    List<UploadedImage> findByUserId(UUID userId);
}
