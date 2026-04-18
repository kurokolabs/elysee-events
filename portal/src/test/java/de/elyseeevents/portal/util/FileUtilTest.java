package de.elyseeevents.portal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    @Test
    void sanitizeFilenameHandlesNullAndBlank() {
        assertEquals("unnamed", FileUtil.sanitizeFilename(null));
        assertEquals("unnamed", FileUtil.sanitizeFilename(""));
        assertEquals("unnamed", FileUtil.sanitizeFilename("   "));
    }

    @Test
    void sanitizeFilenameStripsPathTraversal() {
        // Paths.get().getFileName() already discards parent segments; remaining separators get replaced.
        String result = FileUtil.sanitizeFilename("../../etc/passwd");
        assertFalse(result.contains(".."));
        assertFalse(result.contains("/"));
        assertFalse(result.contains("\\"));
    }

    @Test
    void sanitizeFilenameStripsNullBytesAndControlChars() {
        String nasty = "inv\u0000oice\u0007\u001f.pdf";
        String cleaned = FileUtil.sanitizeFilename(nasty);
        assertEquals("invoice.pdf", cleaned);
    }

    @Test
    void sanitizeFilenameStripsForwardAndBackSlashes() {
        assertEquals("b_file.pdf", FileUtil.sanitizeFilename("a/b_file.pdf").replaceAll("^a_", "b_"));
        String windowsPath = "C:\\Users\\x\\evil.exe";
        String cleaned = FileUtil.sanitizeFilename(windowsPath);
        assertFalse(cleaned.contains("\\"));
        assertFalse(cleaned.contains("/"));
    }

    @Test
    void getFileExtensionReturnsLastDotSegment() {
        assertEquals("pdf", FileUtil.getFileExtension("invoice.pdf"));
        assertEquals("pdf", FileUtil.getFileExtension("my.archive.pdf"));
        assertEquals("", FileUtil.getFileExtension("noextension"));
        assertEquals("", FileUtil.getFileExtension(null));
    }

    @Test
    void allowedMimeTypesMatchWhitelist() {
        assertTrue(FileUtil.isAllowedMimeType("application/pdf"));
        assertTrue(FileUtil.isAllowedMimeType("image/jpeg"));
        assertTrue(FileUtil.isAllowedMimeType("image/png"));
        assertTrue(FileUtil.isAllowedMimeType("application/msword"));
    }

    @Test
    void disallowedMimeTypesRejected() {
        assertFalse(FileUtil.isAllowedMimeType(null));
        assertFalse(FileUtil.isAllowedMimeType(""));
        assertFalse(FileUtil.isAllowedMimeType("application/x-msdownload")); // .exe
        assertFalse(FileUtil.isAllowedMimeType("text/html"));
        assertFalse(FileUtil.isAllowedMimeType("application/javascript"));
        assertFalse(FileUtil.isAllowedMimeType("image/svg+xml")); // SVG can carry scripts
    }

    @Test
    void validateAcceptsAllowedFile() {
        assertDoesNotThrow(() -> FileUtil.validate("invoice.pdf", 1024));
    }

    @Test
    void validateRejectsOversizedFile() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FileUtil.validate("big.pdf", FileUtil.MAX_FILE_SIZE + 1));
        assertTrue(ex.getMessage().toLowerCase().contains("gross"));
    }

    @Test
    void validateRejectsDisallowedExtension() {
        assertThrows(IllegalArgumentException.class, () -> FileUtil.validate("shell.sh", 100));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.validate("evil.exe", 100));
        assertThrows(IllegalArgumentException.class, () -> FileUtil.validate("page.html", 100));
    }
}
