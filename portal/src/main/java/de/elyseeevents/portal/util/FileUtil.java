package de.elyseeevents.portal.util;

import java.nio.file.Paths;
import java.util.Set;

public final class FileUtil {

    public static final Set<String> ALLOWED_TYPES = Set.of(
            "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx");
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private FileUtil() {}

    public static boolean isAllowedMimeType(String contentType) {
        return contentType != null && ALLOWED_MIME_TYPES.contains(contentType);
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "unnamed";
        // Strip null bytes and control characters
        String cleaned = originalFilename.replaceAll("[\\x00-\\x1F\\x7F]", "");
        if (cleaned.isBlank()) return "unnamed";
        String name = Paths.get(cleaned).getFileName().toString();
        // Remove any remaining path-separator-like characters
        name = name.replaceAll("[/\\\\]", "_");
        return name;
    }

    public static void validate(String filename, long size) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Dateiname fehlt.");
        }
        if (size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Datei zu gross (max. 10 MB).");
        }
        String ext = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new IllegalArgumentException("Dateityp nicht erlaubt.");
        }
    }
}
