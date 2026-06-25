package com.ezlearning.service;

import com.ezlearning.model.dto.ReasoningRequest;
import com.ezlearning.model.dto.ReasoningResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfExportServiceImpl implements PdfExportService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 50;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float MAX_IMAGE_WIDTH = 500;
    private static final float TITLE_SIZE = 18;
    private static final float HEADING_SIZE = 14;
    private static final float BODY_SIZE = 11;
    private static final float FOOTER_SIZE = 8;
    private static final float LEADING = 1.4f;

    private final MediaService mediaService;
    private final Map<String, byte[]> pdfCache;

    public PdfExportServiceImpl(MediaService mediaService) {
        this.mediaService = mediaService;
        this.pdfCache = new ConcurrentHashMap<>();
    }

    @Override
    public byte[] exportChat(UUID messageId, ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds) {
        return generatePdf(request, response, mediaIds, false);
    }

    @Override
    public byte[] exportWithImages(UUID messageId, ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds) {
        return generatePdf(request, response, mediaIds, true);
    }

    private byte[] generatePdf(ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds, boolean embedImages) {
        var contentHash = computeHash(request, response, mediaIds, embedImages);
        var cached = pdfCache.get(contentHash);
        if (cached != null) {
            return cached;
        }

        try (var document = new PDDocument()) {
            var writer = new PdfDocumentWriter(document);

            writer.writeHeader();
            writer.writeQuestionSection(request);
            writer.writeAnswerSection(response);

            if (mediaIds != null && !mediaIds.isEmpty() && embedImages) {
                writer.writeDiagramSection(mediaIds);
            }

            writer.writeFooter();
            writer.close();

            var baos = new ByteArrayOutputStream();
            document.save(baos);
            var result = baos.toByteArray();
            pdfCache.put(contentHash, result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private String computeHash(ReasoningRequest request, ReasoningResponse response, List<UUID> mediaIds, boolean embedImages) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var sb = new StringBuilder();
            sb.append(request.question()).append('|')
                    .append(request.context()).append('|')
                    .append(response.answer()).append('|')
                    .append(response.steps()).append('|')
                    .append(response.confidence()).append('|')
                    .append(mediaIds).append('|')
                    .append(embedImages);
            digest.update(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private class PdfDocumentWriter implements AutoCloseable {

        private final PDDocument document;
        private final PDType1Font fontBold;
        private final PDType1Font fontRegular;
        private PDPage currentPage;
        private PDPageContentStream cs;
        private float yCursor;

        PdfDocumentWriter(PDDocument document) throws IOException {
            this.document = document;
            this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            startNewPage();
        }

        void startNewPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            cs = new PDPageContentStream(document, currentPage);
            yCursor = PAGE_HEIGHT - MARGIN;
        }

        void ensureSpace(float needed) throws IOException {
            if (yCursor - needed < MARGIN + FOOTER_SIZE * 2) {
                startNewPage();
            }
        }

        void writeHeader() throws IOException {
            cs.beginText();
            cs.setFont(fontBold, TITLE_SIZE);
            cs.newLineAtOffset(MARGIN, yCursor);
            cs.showText("EzLearning - Exportacao de Duvida");
            cs.endText();
            yCursor -= TITLE_SIZE * LEADING;

            var dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            cs.beginText();
            cs.setFont(fontRegular, BODY_SIZE);
            cs.newLineAtOffset(MARGIN, yCursor);
            cs.showText(dateStr);
            cs.endText();
            yCursor -= BODY_SIZE * LEADING;
        }

        void writeQuestionSection(ReasoningRequest request) throws IOException {
            ensureSpace(HEADING_SIZE * 2);
            writeSectionTitle("Sua Duvida");
            yCursor -= 5;
            writeBodyText(request.question());
            if (request.context() != null && !request.context().isBlank()) {
                yCursor -= 8;
                writeBodyText("Contexto: " + request.context());
            }
        }

        void writeAnswerSection(ReasoningResponse response) throws IOException {
            yCursor -= 15;
            ensureSpace(HEADING_SIZE * 2);
            writeSectionTitle("Resposta");
            yCursor -= 5;
            writeBodyText(response.answer());

            if (response.steps() != null && !response.steps().isEmpty()) {
                yCursor -= 8;
                for (var i = 0; i < response.steps().size(); i++) {
                    writeBodyText((i + 1) + ". " + response.steps().get(i));
                }
            }
        }

        void writeDiagramSection(List<UUID> mediaIds) throws IOException {
            yCursor -= 15;
            ensureSpace(HEADING_SIZE * 2);
            writeSectionTitle("Diagramas");

            for (var mediaId : mediaIds) {
                try {
                    var imageBytes = mediaService.loadMedia(mediaId);
                    var image = PDImageXObject.createFromByteArray(document, imageBytes, mediaId.toString());
                    var scale = Math.min(1.0f, MAX_IMAGE_WIDTH / image.getWidth());
                    var scaledWidth = image.getWidth() * scale;
                    var scaledHeight = image.getHeight() * scale;

                    ensureSpace(scaledHeight + 15);
                    yCursor -= scaledHeight + 10;
                    cs.drawImage(image, MARGIN, yCursor, scaledWidth, scaledHeight);
                } catch (Exception e) {
                    writeBodyText("[Imagem indisponivel: " + mediaId + "]");
                }
            }
        }

        void writeFooter() throws IOException {
            if (cs != null) {
                cs.beginText();
                cs.setFont(fontRegular, FOOTER_SIZE);
                cs.newLineAtOffset(MARGIN, MARGIN);
                cs.showText("Gerado por EzLearning - Tutor Digital Personalizado");
                cs.endText();
            }
        }

        private static String sanitizeForHelvetica(String text) {
            return text
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replace("\n", " ")
                    .replace("\t", " ")
                    .replaceAll("[\\p{Cntrl}]", "")
                    .replaceAll("[^\\u0020-\\u007E\\u00A0-\\u00FF]", "?")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        private void writeSectionTitle(String title) throws IOException {
            cs.beginText();
            cs.setFont(fontBold, HEADING_SIZE);
            cs.newLineAtOffset(MARGIN, yCursor);
            cs.showText(title);
            cs.endText();
            yCursor -= HEADING_SIZE * LEADING;
        }

        private void writeBodyText(String text) throws IOException {
            text = sanitizeForHelvetica(text);
            var lines = wrapText(text, fontRegular, BODY_SIZE);
            for (var line : lines) {
                ensureSpace(BODY_SIZE * LEADING);
                cs.beginText();
                cs.setFont(fontRegular, BODY_SIZE);
                cs.newLineAtOffset(MARGIN, yCursor);
                cs.showText(line);
                cs.endText();
                yCursor -= BODY_SIZE * LEADING;
            }
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize) throws IOException {
            var lines = new ArrayList<String>();
            var words = text.split(" ");
            var currentLine = new StringBuilder();

            for (var word : words) {
                var testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                var lineWidth = font.getStringWidth(testLine) / 1000f * fontSize;
                if (lineWidth > CONTENT_WIDTH && !currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    if (!currentLine.isEmpty()) {
                        currentLine.append(' ');
                    }
                    currentLine.append(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            if (lines.isEmpty()) {
                lines.add("");
            }

            return lines;
        }

        @Override
        public void close() throws IOException {
            if (cs != null) {
                cs.close();
            }
        }
    }
}
