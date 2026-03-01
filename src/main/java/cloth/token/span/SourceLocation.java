package cloth.token.span;

import cloth.file.SourceFile;

/**
 * Represents a specific location within a source file.
 * This includes the file, character offset, line number, and column number.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public record SourceLocation(SourceFile file, int offset, int line, int column) {
}
