package cloth.token;

import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a token in the source code, containing information about its kind,
 * associated keyword or operator (if any), location in the source, and the text
 * representation.
 *
 * @param kind    The type of the token (e.g., keyword, operator, identifier, etc.).
 * @param keyword The keyword associated with the token, if it represents a keyword.
 * @param operator The operator associated with the token, if it represents an operator.
 * @param span    The location of the token in the source code.
 * @param lexeme  The textual representation of the token from the source.
 */
public record Token(TokenKind kind, Tokens.Keyword keyword, Tokens.Operator operator, SourceSpan span, String lexeme) implements IToken {

    @Override
    public boolean is(TokenKind kind) {
        return this.kind == kind;
    }

    @Override
    public SourceLocation getLocation() {
        return span.start();
    }

    @Override
    public @NotNull String toString() {
        return lexeme;
    }

}
