package com.ezlearning.repository;

import com.ezlearning.model.GeneratedMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedMediaRepository extends JpaRepository<GeneratedMedia, UUID> {

    Optional<GeneratedMedia> findByPromptHash(String promptHash);

    List<GeneratedMedia> findByReferencedFalseAndCreatedAtBefore(LocalDateTime threshold);

    List<GeneratedMedia> findByCreatedAtBefore(LocalDateTime threshold);
}
