package com.portal.learningportal.service;

/**
 * Sanitizes original upload filenames for Cloudinary {@code filename_override}.
 */
public final class FileDownloadNames {

    private FileDownloadNames() {
    }

    public static String sanitizedOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String name = originalFilename;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("^-|-$", "");
        if (name.isBlank() || name.length() > 180) {
            return null;
        }
        return name;
    }
}