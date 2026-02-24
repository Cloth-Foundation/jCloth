package cloth.lexer.trivia;

import cloth.token.TokenKind;
import cloth.token.span.SourceSpan;

/**
 * Represents a piece of trivia within a source file, which includes non-essential
 * elements such as whitespace or comments. Trivia pieces are typically used during
 * lexical analysis to capture contextual information surrounding tokens, but they
 * are not part of the core syntax or semantics of the language.
 *
 * @param kind The kind of the trivia, indicating its type (e.g., whitespace, comment).
 * @param span The span in the source file where the trivia occurs.
 * @param text The textual representation of the trivia as it appears in the source file.
 */
public record TriviaPiece(TokenKind kind, SourceSpan span, String text) {

    public TriviaPiece(SourceSpan span, String text) {
        this(TokenKind.Whitespace, span, text);
    }

}
