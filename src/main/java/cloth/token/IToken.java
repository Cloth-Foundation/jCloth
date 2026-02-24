package cloth.token;

import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;

/**
 * Represents a token identified in the source code. A token is a unit of the code
 * parsed during lexical analysis, and this interface provides methods to access its
 * properties, such as its type, text representation, and location in the source.
 */
public interface IToken {

    TokenKind kind();

    SourceSpan span();

    String lexeme();

    default boolean is(TokenKind kind) {
        return kind() == kind;
    }

    default SourceLocation getLocation() {
        return span().start();
    }
}
