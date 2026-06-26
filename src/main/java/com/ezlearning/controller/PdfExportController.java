package com.ezlearning.controller;

import com.ezlearning.model.dto.PdfExportRequest;
import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import com.ezlearning.service.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class PdfExportController {

    private final PdfExportService pdfExportService;

    public PdfExportController(PdfExportService pdfExportService) {
        this.pdfExportService = pdfExportService;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportPdfSimple(@RequestBody PdfExportRequest request) {
        var messageId = UUID.randomUUID();
        var reasoningRequest = new ReasoningRequest(request.question(), request.context());
        var reasoningResponse = new ReasoningResponse(request.answer(), request.steps(), request.confidence());

        byte[] pdfBytes = pdfExportService.exportChat(messageId, reasoningRequest, reasoningResponse, request.mediaIds());

        var dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        var filename = "duvida-" + dateStr + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{messageId}/export")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable UUID messageId,
            @RequestBody PdfExportRequest request) {
        var reasoningRequest = new ReasoningRequest(request.question(), request.context());
        var reasoningResponse = new ReasoningResponse(request.answer(), request.steps(), request.confidence());

        byte[] pdfBytes;
        if (request.mediaIds() != null && !request.mediaIds().isEmpty()) {
            pdfBytes = pdfExportService.exportWithImages(messageId, reasoningRequest, reasoningResponse, request.mediaIds());
        } else {
            pdfBytes = pdfExportService.exportChat(messageId, reasoningRequest, reasoningResponse, request.mediaIds());
        }

        var dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        var filename = "duvida-" + dateStr + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
