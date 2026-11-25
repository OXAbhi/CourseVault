package com.portal.learningportal.service;

import com.portal.learningportal.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PdfTextExtractorService {

    private static final int MAX_CHARS_FOR_PROMPT = 14_000;

    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BadRequestException("PDF file is empty.");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();
            if (text.isBlank()) {
                throw new BadRequestException("No readable text found in this PDF (scanned image PDFs need OCR).");
            }
            if (text.length() > MAX_CHARS_FOR_PROMPT) {
                return text.substring(0, MAX_CHARS_FOR_PROMPT);
            }
            return text;
        } catch (IOException ex) {
            throw new BadRequestException("Could not read PDF: " + ex.getMessage());
        }
    }
}