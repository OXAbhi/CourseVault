package com.portal.learningportal.service;

import com.portal.learningportal.exception.BadRequestException;
import com.portal.learningportal.exception.UnprocessableEntityException;
import com.portal.learningportal.util.PdfPageRange;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PdfTextExtractorService {

    public static final int MIN_CHARS = 300;
    private static final int MAX_CHARS_FOR_PROMPT = 18_000;

    public int pageCount(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BadRequestException("PDF file is empty.");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read PDF: " + ex.getMessage());
        }
    }

    public String extractText(byte[] pdfBytes, String pagesSpec) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BadRequestException("PDF file is empty.");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            PdfPageRange range = PdfPageRange.parse(pagesSpec, totalPages);
            PDFTextStripper stripper = new PDFTextStripper();
            if (range != null) {
                stripper.setStartPage(range.startPage());
                stripper.setEndPage(range.endPage());
            } else {
                // Skip heavy front matter when using full document: start at page 3 if long enough
                if (totalPages > 6) {
                    stripper.setStartPage(3);
                }
            }
            String text = stripper.getText(document).trim();
            if (text.isBlank()) {
                throw new BadRequestException("No readable text found in this PDF (scanned image PDFs need OCR).");
            }
            if (text.length() < MIN_CHARS) {
                throw new UnprocessableEntityException("Not enough content in this file");
            }
            if (text.length() > MAX_CHARS_FOR_PROMPT) {
                return text.substring(0, MAX_CHARS_FOR_PROMPT);
            }
            return text;
        } catch (UnprocessableEntityException | BadRequestException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BadRequestException("Could not read PDF: " + ex.getMessage());
        }
    }
}