package cloth.file;

import lombok.Getter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a source file by extracting and organizing its path, name, directory,
 * base name, and file extension. This class provides utility methods to validate
 * the file based on its extension and ensures compatibility with specific file
 * formats.
 */
public class SourceFile {

    @Getter
    private final String path;

    @Getter
    private final String name;

    @Getter
    private final String directory;

    @Getter
    private final String baseName;

    @Getter
    private final String extension;

    public static final String CLOTH_EXTENSION = "co";

    public SourceFile(String path) {
        this.path = normalizePath(path);

        int lastSlash = Math.max(this.path.lastIndexOf('/'), this.path.lastIndexOf('\\'));
        this.name = lastSlash >= 0 ? this.path.substring(lastSlash + 1) : this.path;
        this.directory = lastSlash >= 0 ? this.path.substring(0, lastSlash) : "";

        int lastDot = this.name.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < this.name.length()) {
            this.baseName = this.name.substring(0, lastDot);
            this.extension = this.name.substring(lastDot + 1);
        } else {
            this.baseName = this.name;
            this.extension = "";
        }
    }

    public boolean hasExtension() {
        return !extension.isEmpty();
    }

    public boolean hasExtension(String ext) {
        if (ext == null) return false;
        String normalized = normalizeExtension(ext);
        return !normalized.isEmpty() && extension.equalsIgnoreCase(normalized);
    }

    public boolean isClothObjectFile() {
        return hasExtension(CLOTH_EXTENSION);
    }

    public void validateClothObjectFile() {
        if (!isClothObjectFile()) {
            throw new IllegalArgumentException("Expected a ." + CLOTH_EXTENSION + " file, got: " + name);
        }
    }

    public String getSourceText() {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read source file: " + path, e);
        }
    }

    private static String normalizeExtension(String ext) {
        String e = ext.trim();
        if (e.startsWith(".")) e = e.substring(1);
        return e;
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        return path.trim();
    }
    
    public String getAbsolutePath() {
        return Path.of(path).toAbsolutePath().toString();
    }

    public File getFile() {
        return new File(path);
    }

}
