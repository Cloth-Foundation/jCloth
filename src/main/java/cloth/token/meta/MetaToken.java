package cloth.token.meta;

import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a specialized token in the source code that belongs to the "Meta" category.
 * A MetaToken is a subclass of {@link IToken} and is specifically used to identify
 * metaprogramming keywords or constructs within the source code.
 * <p>
 * Each MetaToken encapsulates the token's type (kind), the span of the source code
 * it corresponds to, its lexical value (lexeme), and an optional meta keyword.
 *
 * @param kind    The {@link TokenKind} indicating the type of this token, which is always {@code TokenKind.Meta}.
 * @param span    A {@link SourceSpan} object representing where in the source code the token resides.
 * @param lexeme  The raw string representation of the token as found in the source code.
 * @param keyword The {@link MetaKeyword} associated with this token, if applicable.
 */
public record MetaToken(TokenKind kind, SourceSpan span, String lexeme, MetaKeyword keyword) implements IToken {

    /**
     * Checks if the token has the specified {@code TokenKind}.
     *
     * @param kind the {@code TokenKind} to compare against.
     * @return {@code true} if the token's kind matches the specified {@code TokenKind}, {@code false} otherwise.
     */
    @Override
    public boolean is(TokenKind kind) {
        return this.kind == kind;
    }

    /**
     * Retrieves the starting location of the source span for this token.
     * The location corresponds to the {@code SourceLocation} where the token begins
     * within the source file, providing details such as file reference, offset, line,
     * and column position.
     *
     * @return the {@code SourceLocation} representing the starting position
     *         of the token in the source code.
     */
    @Override
    public SourceLocation getLocation() {
        return span.start();
    }

    /**
     * Returns the string representation of this token, which corresponds to
     * its lexeme as it appears in the source code.
     *
     * @return the lexeme of this token as a non-null string.
     */
    @Override
    public @NotNull String toString() {
        return lexeme;
    }

}
