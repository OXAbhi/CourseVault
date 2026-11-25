package com.portal.learningportal.util;

import com.portal.learningportal.exception.BadRequestException;

public record PdfPageRange(int startPage, int endPage) {

    private static final int MAX_PAGES = 40;

    public static PdfPageRange parse(String pages, int documentPageCount) {
        if (pages == null || pages.isBlank()) {
            return null;
        }
        String normalized = pages.trim().replaceAll("\\s+", "");
        String[] parts = normalized.split("-");
        if (parts.length == 1) {
            int page = parsePage(parts[0]);
            return clamp(page, page, documentPageCount);
        }
        if (parts.length == 2) {
            int start = parsePage(parts[0]);
            int end = parsePage(parts[1]);
            if (end < start) {
                throw new BadRequestException("Invalid page range: end must be >= start.");
            }
            return clamp(start, end, documentPageCount);
        }
        throw new BadRequestException("Invalid pages format. Use e.g. 12 or 12-20.");
    }

    private static int parsePage(String token) {
        try {
            int page = Integer.parseInt(token);
            if (page < 1) {
                throw new BadRequestException("Page numbers start at 1.");
            }
            return page;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid page number: " + token);
        }
    }

    private static PdfPageRange clamp(int start, int end, int documentPageCount) {
        if (documentPageCount > 0) {
            if (start > documentPageCount) {
                throw new BadRequestException("Start page exceeds document length (" + documentPageCount + " pages).");
            }
            end = Math.min(end, documentPageCount);
        }
        if (end - start + 1 > MAX_PAGES) {
            throw new BadRequestException("Page range too large (max " + MAX_PAGES + " pages per generation).");
        }
        return new PdfPageRange(start, end);
    }
}