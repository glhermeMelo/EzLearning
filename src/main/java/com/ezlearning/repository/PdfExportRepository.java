package com.ezlearning.repository;

import com.ezlearning.model.PdfExport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdfExportRepository extends JpaRepository<PdfExport, UUID> {

    Optional<PdfExport> findByContentHash(String contentHash);

    List<PdfExport> findByMessageId(UUID messageId);
}
