package cloth.lexer;

import cloth.lexer.trivia.Trivia;
import cloth.token.IToken;

/**
 * Represents a token in the lexing process, encapsulating both the token itself and
 * its associated trivia. Lexed tokens are produced during lexical analysis and include
 * the primary token data along with additional surrounding contextual information,
 * such as leading and trailing trivia.
 *
 * @param token  The core lexical token, providing data such as token type, text, and location.
 * @param trivia The trivia associated with the token, which includes leading and trailing
 *               non-essential elements like whitespace or comments.
 */
public record LexedToken(IToken token, Trivia trivia) {
}
