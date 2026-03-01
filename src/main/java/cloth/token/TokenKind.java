package cloth.token;

/**
 * Represents the various kinds of tokens that can be identified in the source code.
 * Each token kind corresponds to a specific type of lexical element in the language.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum TokenKind {

    // Common
    EndOfFile,
    Error,

    // Literals
    Identifier,
    Number,
    String,

    // Punctuation
    Operator,
    Punctuation,

    // Types
    Keyword,
    Whitespace,
    Comment,

    // Meta Access Tokens
    Meta

}
