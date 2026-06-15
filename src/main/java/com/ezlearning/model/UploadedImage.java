package com.ezlearning.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "uploaded_images")
public class UploadedImage {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Setter
    @Column(name = "stored_path", nullable = false)
    private String storedPath;

    @Setter
    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Setter
    @Column(nullable = false)
    private long size;

    @Setter
    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "user_id")
    private UUID userId;

    public UploadedImage() {}

    public UploadedImage(String originalName, String storedPath, String thumbnailPath, long size, String mimeType, UUID userId) {
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.thumbnailPath = thumbnailPath;
        this.size = size;
        this.mimeType = mimeType;
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
