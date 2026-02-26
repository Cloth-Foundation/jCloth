package cloth.token;

import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;

/**
 * Represents a token identified in the source code. A token is a unit of the code
 * parsed during lexical analysis, and this interface provides methods to access its
 * properties, such as its type, text representation, and location in the source.
 */
public interface IToken {

    /**
     * Retrieves the kind of the token, which describes its role or category in the
     * source code. The token kind indicates whether the token is an identifier,
     * literal, keyword, operator, punctuation, or any other recognized element
     * in the lexical analysis phase.
     *
     * @return the {@code TokenKind} representing the category of the token.
     */
    TokenKind kind();

    /**
     * Retrieves the span of the token in the source code.
     * The span represents the range in the source file where
     * the token is located, defined by a starting and ending
     * {@code SourceLocation}.
     *
     * @return the {@code SourceSpan} indicating the start and end
     *         positions of the token within the source code.
     */
    SourceSpan span();

    /**
     * Retrieves the textual representation of the token as it appears in the source code.
     * The lexeme includes the exact characters from the source that compose the token,
     * such as an identifier, keyword, operator, or literal value.
     *
     * @return the source code fragment corresponding to the token's lexeme.
     */
    String lexeme();

    /**
     * Checks if the token matches the specified {@code TokenKind}.
     *
     * @param kind the {@code TokenKind} to compare against.
     * @return {@code true} if the token's kind matches the specified {@code TokenKind}, {@code false} otherwise.
     */
    default boolean is(TokenKind kind) {
        return kind() == kind;
    }

    /**
     * Retrieves the starting location of the token in the source code.
     *
     * @return the {@code SourceLocation} representing the position where the token starts.
     */
    default SourceLocation getLocation() {
        return span().start();
    }
}
