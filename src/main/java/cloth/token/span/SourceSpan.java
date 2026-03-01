package cloth.token.span;

/**
 * Represents a span within a source file, defined by a start and end location.
 * This class provides methods to validate the span and determine its length.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 *
 * @param start The starting location of the span in the source file.
 * @param end   The ending location of the span in the source file.
 */
public record SourceSpan(SourceLocation start, SourceLocation end) {

    public boolean isValid() {
        return start().file() == end().file() && end().offset() >= start().offset();
    }

    public int getLength() {
        return isValid() ? end().offset() - start().offset() : 0;
    }

}
