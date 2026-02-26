package cloth.file;

import lombok.Getter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a source file with metadata and utility methods for path and extension handling.
 * Provides access to details such as the file name, directory, base name, and file extension.
 * Includes functionality to validate specific file formats and access file content.
 */
public class SourceFile {

    /**
     * The file path representing the location of the source file.
     * This value is immutable and normalized to remove leading or trailing
     * whitespace upon initialization.
     * <p>
     * The `path` is used to derive additional file metadata, such as the
     * file name, directory, extension, and base name.
     * <p>
     * It can be used to access the file on the filesystem, validate its type,
     * or retrieve its content when necessary.
     */
    @Getter
    private final String path;

    /**
     * The file name, excluding its path and extension.
     * This field represents the name component of a source file
     * after separating it from the directory structure and file extension.
     * It is a final, immutable property of the object.
     */
    @Getter
    private final String name;

    /**
     * Represents the directory of the source file. It is a final and immutable string
     * that holds the normalized path to the directory containing the source file.
     * <p>
     * This variable is initialized during the creation of a {@code SourceFile} instance
     * and cannot be modified thereafter.
     * <p>
     * The directory value is typically used to perform operations related to locating
     * the source file on the filesystem, managing paths, and supporting validation
     * tasks within the {@code SourceFile} class.
     */
    @Getter
    private final String directory;

    /**
     * Represents the base name of the source file without its directory path or extension.
     * It is a finalized string that denotes the core name of the file after being extracted
     * and normalized during initialization of the SourceFile object.
     * <p>
     * This field is immutable and can only be accessed through the provided getter method.
     */
    @Getter
    private final String baseName;

    /**
     * Represents the file extension of the source file.
     * The extension is typically normalized to ensure consistency (e.g.,
     * leading periods are removed). It can be used to check the file's type
     * or to determine if the file matches specific criteria such as a
     * recognized custom extension.
     * <p>
     * This field is immutable and initialized during the creation of the
     * {@code SourceFile} object. It plays a key role in methods such as
     * {@code hasExtension} and {@code isClothObjectFile}, which facilitate
     * operations based on the file's extension.
     */
    @Getter
    private final String extension;

    /**
     * Represents the file extension used for identifying cloth object files.
     * This constant is utilized within methods to validate and determine if a file
     * has the `.co` extension, which is specific to cloth object files in this context.
     */
    public static final String CLOTH_EXTENSION = "co";

    /**
     * Constructs a {@code SourceFile} instance by extracting and normalizing file path
     * components such as the directory, file name, base name (name without extension),
     * and file extension from the provided file path.
     *
     * @param path The file path to be processed. This can be either an absolute or
     *             relative path and may include directory separators such as '/' or '\\'.
     */
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

    /**
     * Checks if the current file has a non-empty extension.
     *
     * @return {@code true} if the file has an extension, {@code false} otherwise.
     */
    public boolean hasExtension() {
        return !extension.isEmpty();
    }

    /**
     * Checks if the provided extension matches the file's extension after normalization.
     *
     * @param ext The file extension to check. It may include or exclude the leading dot (e.g., ".txt" or "txt").
     *            A {@code null} or empty string will result in {@code false}.
     * @return {@code true} if the provided extension matches the file's extension, {@code false} otherwise.
     */
    public boolean hasExtension(String ext) {
        if (ext == null) return false;
        String normalized = normalizeExtension(ext);
        return !normalized.isEmpty() && extension.equalsIgnoreCase(normalized);
    }

    /**
     * Determines if the current file is a cloth object file by checking its extension.
     *
     * @return {@code true} if the file's extension matches the predefined cloth object file extension,
     *         {@code false} otherwise.
     */
    public boolean isClothObjectFile() {
        return hasExtension() && hasExtension(CLOTH_EXTENSION);
    }

    /**
     * Validates whether the current file is a cloth object file by checking its extension.
     * If the file's extension does not match the predefined cloth object file extension,
     * an {@code IllegalArgumentException} is thrown.
     *
     * @throws IllegalArgumentException if the file is not a cloth object file or the extension is invalid.
     */
    public void validateClothObjectFile() {
        if (!isClothObjectFile()) {
            throw new IllegalArgumentException("Expected a ." + CLOTH_EXTENSION + " file, got: " + name);
        }
    }

    /**
     * Retrieves the text content of the source file specified by the path.
     * The method reads the file using UTF-8 encoding and returns its content as a string.
     * If any exception occurs while reading the file, a {@code RuntimeException} is thrown.
     *
     * @return the content of the source file as a {@code String}.
     * @throws RuntimeException if the file cannot be read due to I/O errors or other issues.
     */
    public String getSourceText() {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read source file: " + path, e);
        }
    }

    /**
     * Normalizes a file extension by trimming leading and trailing whitespace and
     * removing the leading dot (if present).
     *
     * @param ext The file extension to normalize. It may include a leading dot (e.g., ".txt")
     *            or be without it (e.g., "txt"). A {@code null} input is not expected.
     * @return The normalized file extension as a non-null, trimmed string without a leading dot.
     */
    private static String normalizeExtension(String ext) {
        String e = ext.trim();
        if (e.startsWith(".")) e = e.substring(1);
        return e;
    }

    /**
     * Normalizes a file path by trimming leading and trailing whitespace.
     * If the provided path is {@code null}, an empty string is returned.
     *
     * @param path The file path to normalize. This may include directory
     *             separators such as '/' or '\\'. A {@code null} input
     *             will result in an empty string.
     * @return The normalized file path as a trimmed string, or an empty
     *         string if the input is {@code null}.
     */
    private static String normalizePath(String path) {
        if (path == null) return "";
        return path.trim();
    }

    /**
     * Retrieves the absolute path of the file represented by this object.
     *
     * @return The absolute file path as a {@code String}.
     */
    public String getAbsolutePath() {
        return Path.of(path).toAbsolutePath().toString();
    }

    /**
     * Retrieves a {@code File} object corresponding to the file path
     * represented by the current {@code SourceFile} instance.
     *
     * @return a {@code File} object that represents the file path stored
     *         in this {@code SourceFile}.
     */
    public File getFile() {
        return new File(path);
    }

}
