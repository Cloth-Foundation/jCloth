package cloth.lexer;

import cloth.error.DiagnosticSink;
import cloth.lexer.trivia.Trivia;
import cloth.lexer.trivia.TriviaPiece;
import cloth.token.IToken;
import cloth.token.Token;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.meta.MetaKeyword;
import cloth.token.meta.MetaToken;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The Lexer class tokenizes the input text from a source buffer according to the
 * language's lexical grammar. It processes the input character stream, identifies
 * tokens, and provides functionality for lookahead and consuming tokens.
 * <p>
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public final class Lexer {

    public static final boolean kNestedBlockComments = true;

    private final int kLookahead = 8;
    private int lookaheadIndex = 0;
    private int lookaheadCount = 0;
    private final LexedToken[] lookaheadTokens = new LexedToken[kLookahead];

    private final char[] text;
    private int current;
    private final int end;

    @Getter
    private int offset;

    @Getter
    private int line;

    @Getter
    private int column;

    private int tokenBegin;
    private int tokenOffset;
    private int tokenLine;
    private int tokenColumn;

    @Getter
    private final SourceBuffer buffer;

    @Getter
    private final DiagnosticSink diagnostics;

    @Getter
    private final LexerOptions options;

    @Nullable
    private IToken previousToken = null;

    public Lexer(SourceBuffer buffer, DiagnosticSink diagnostics, LexerOptions options) {
        this.buffer = buffer;
        this.diagnostics = diagnostics;
        this.options = options;

        this.text = buffer.getText().toCharArray();
        this.current = 0;
        this.end = text.length;

        this.offset = 0;
        this.line = 1;
        this.column = 1;

        this.tokenBegin = 0;
        this.tokenOffset = 0;
        this.tokenLine = 1;
        this.tokenColumn = 1;
    }

    public IToken getPreviousToken() {
        return previousToken;
    }

    /**
     * Retrieves a lookahead lexical token at a specified position relative to the current
     * head of the lookahead buffer.
     * <p>
     * This method ensures that the lookahead buffer contains at least the required number
     * of tokens by invoking the {@code fillLookAhead} method. The token is then accessed
     * from the buffer using a calculated index.
     *
     * @param n The zero-based index of the lookahead token to retrieve, relative to the
     *          current head of the lookahead buffer. Must be a non-negative integer.
     * @return The lookahead token at the specified position, represented as a
     *         {@code LexedToken} instance.
     */
    public LexedToken peek(int n) {
        fillLookAhead(n + 1);
        return lookaheadTokens[(lookaheadIndex + n) % kLookahead];
    }

    /**
     * Retrieves the next lexical token from the lexer's lookahead buffer.
     * This method ensures that the buffer is populated with at least one token
     * using the {@code fillLookAhead(int need)} method, extracts the token
     * located at the current lookahead position, and updates the lookahead
     * state accordingly.
     *
     * @return The next lexical token as a {@code LexedToken} instance. The returned token
     *         encapsulates both the core lexical data and any associated trivia.
     */
    public LexedToken next() {
        fillLookAhead(1);
        LexedToken out = lookaheadTokens[lookaheadIndex];
        lookaheadIndex = (lookaheadIndex + 1) % kLookahead;
        lookaheadCount--;
        previousToken = out.token();
        return out;
    }

    /**
     * Checks if the lexer has reached the end of the input.
     * This method determines if the current token is of the kind {@code TokenKind.EndOfFile}.
     *
     * @return true if the current token is of kind {@code TokenKind.EndOfFile}, false otherwise.
     */
    public boolean eof() {
        return peek(0).token().kind() == TokenKind.EndOfFile;
    }

    /**
     * Determines whether the lexer has reached the end of the input.
     * This method checks if the current position in the input stream
     * is greater than or equal to the designated end position.
     *
     * @return true if the current position is at or beyond the end of the input, false otherwise.
     */
    public boolean atEnd() {
        return current >= end;
    }

    /**
     * Retrieves the current character at the lexer's current position in the input text.
     * If the current position is at the end of the input, this method returns the null
     * character ('\0').
     *
     * @return The character at the current position in the input text, or '\0' if the position
     *         is at the end of the input.
     */
    public char current() {
        return atEnd() ? '\0' : text[current];
    }

    /**
     * Returns the character at a specified lookahead position relative to the current
     * position in the input text. If the lookahead position is beyond the end of the
     * input, this method returns the null character ('\0').
     *
     * @param n The number of characters to look ahead from the current position.
     *          It must be a non-negative integer.
     * @return The character at the specified lookahead position, or '\0' if the position
     *         exceeds the end of the input.
     */
    private char lookaheadChar(int n) {
        int p = current + n;
        return (p >= end) ? '\0' : text[p];
    }

    /**
     * Populates the lookahead buffer with additional tokens until it contains at least the required number
     * specified by the {@code need} parameter. If the lookahead buffer overflows, an error is reported.
     *
     * @param need The minimum number of tokens that must be present in the lookahead buffer after this method
     *             completes. If the buffer already contains this many tokens, no action is taken.
     */
    public void fillLookAhead(int need) {
        while (lookaheadCount < need) {
            if (lookaheadCount >= kLookahead) {
                diagnostics.error(currentLocation(), "Buffer overflow in lexer lookahead.");
                break;
            }

            var tail = (lookaheadIndex + lookaheadCount) % kLookahead;
            lookaheadTokens[tail] = lexOne();
            lookaheadCount++;
        }
    }

    /**
     * Analyzes the current position in the source input and identifies the next lexical token,
     * along with its associated trivia, based on the rules of the lexer.
     * <p>
     * The method processes various token types, including whitespace, identifiers, keywords,
     * numbers, strings, operators, punctuation, comments, and the end-of-file marker.
     * Depending on the lexer options, trivia such as leading and trailing whitespace or comments
     * can be preserved or consumed.
     * <p>
     * The method also manages transitions between tokens, ensuring the lexer state advances
     * correctly with each invocation.
     *
     * @return The next lexical token wrapped in a LexedToken instance, containing the core
     *         token data and any associated trivia as per the lexer configuration.
     */
    private LexedToken lexOne() {
        Trivia trivia = new Trivia();
        trivia.clear();

        if (options.isKeepTrivia() || !options.isEmitWhitespace() || !options.isEmitComments()) consumeTrivia(trivia.leading());

        beginToken();

        if (atEnd()) {
            IToken eofToken = scanEndOfFile();
            return emit(new LexedToken(eofToken, trivia));
        }

        char c = current();

        // Optional: emit whitespace tokens rather than consuming as trivia
        if (options.isEmitWhitespace() && isWhitespace(c)) {
            IToken ws = scanWhitespaceToken();
            maybeConsumeTrailingTrivia(trivia);
            return emit(new LexedToken(ws, trivia));
        }

        // Ident / keyword
        if (isIdentifierStart(c)) {
            IToken id = scanIdentifierOrKeyword();
            maybeConsumeTrailingTrivia(trivia);
            return emit(new LexedToken(id, trivia));
        }

        // Number (starting with a digit; handle .123 in the operator scanner if you allow)
        if (isDigit(c)) {
            IToken num = scanNumber();
            maybeConsumeTrailingTrivia(trivia);
            return emit(new LexedToken(num, trivia));
        }

        // String
        if (c == '"' || c == '\'') {
            IToken str = scanStringLiteral(c);
            maybeConsumeTrailingTrivia(trivia);
            return emit(new LexedToken(str, trivia));
        }

        // Slash: comment or operator
        if (c == '/') {
            IToken slash = scanCommentOrSlashOperator();
            // If comments are tokens, trailing trivia should still attach.
            maybeConsumeTrailingTrivia(trivia);
            return emit(new LexedToken(slash, trivia));
        }

        // Operator / punctuation / unknown
        IToken op = scanOperatorOrPunctuation();
        maybeConsumeTrailingTrivia(trivia);
        return emit(new LexedToken(op, trivia));
    }

    /**
     * Processes and conditionally modifies a given lexical token before returning it.
     * If the lexer options specify that trivia should not be preserved, the associated
     * trivia of the token is cleared.
     *
     * @param token The lexical token to be processed. It encapsulates both the core
     *              token data and its associated trivia, such as leading and trailing
     *              non-essential elements (e.g., whitespace or comments).
     * @return The processed lexical token, potentially with its trivia cleared, depending
     *         on the lexer configuration.
     */
    private LexedToken emit(LexedToken token) {
        if (!options.isKeepTrivia()) token.trivia().clear();
        return token;
    }

    /**
     * Advances the current position in the input text by one character, updating the
     * positional state of the lexer in the process. If the end of the input has been
     * reached (as determined by the {@code atEnd()} method), no action is taken.
     * <p>
     * The consumed character is used to update the lexer's line and column tracking
     * via the {@code bumpLocation(char consumed)} method:
     * - If the character is a newline ('\n'), the line count is incremented and the
     *   column is reset to 1.
     * - For any other character, the column count is incremented.
     * <p>
     * This method is typically used internally to progress through the input stream
     * during lexical analysis.
     */
    private void advance() {
        if (atEnd()) return;
        char consumed = text[current++];
        bumpLocation(consumed);
    }

    /**
     * Advances the current position in the input text by up to {@code n} characters,
     * stopping if the end of the input is reached sooner.
     * <p>
     * This method repeatedly invokes the {@code advance()} method to move the
     * lexer's current position forward in the input text. It checks the
     * termination condition using the {@code atEnd()} method to avoid overshooting
     * the end of the input.
     *
     * @param n The maximum number of characters to advance. If {@code n} is zero
     *          or negative, no advancement occurs. If the end of the input is
     *          reached before {@code n} steps, advancement halts early.
     */
    private void advanceN(int n) {
        while (n-- > 0 && !atEnd()) advance();
    }

    /**
     * Checks if the current character matches the given character and advances the position if it matches.
     *
     * @param c the character to match against the current character
     * @return true if the current character matches the given character, false otherwise
     */
    @SuppressWarnings("unused")
    private boolean match(char c) {
        if (current() != c) return false;
        advance();
        return true;
    }

    /**
     * Checks if the current character matches the first specified character and
     * the next character matches the second specified character. If both conditions
     * are met, advances the position by two characters.
     *
     * @param a the character to match against the current character.
     * @param b the character to match against the next character.
     * @return {@code true} if the current character matches {@code a} and the next
     *         character matches {@code b}; {@code false} otherwise.
     */
    @SuppressWarnings("unused")
    private boolean match2(char a, char b) {
        if (current() == a && lookaheadChar(1) == b) {
            advanceN(2);
            return true;
        }
        return false;
    }

    /**
     * Checks if the provided string matches the substring starting from the current position.
     * Advances the position if the match is successful.
     *
     * @param s the string to match against the current substring
     * @return true if the provided string matches the substring, otherwise false
     */
    private boolean matchString(String s) {
        if (s.isEmpty()) return true;
        int n = s.length();
        if (end - current < n) return false;
        char[] expected = s.toCharArray();
        for (int i = 0; i < n; ++i) {
            if (text[current + i] != expected[i]) return false;
        }

        advanceN(n);
        return true;
    }

    /**
     * Checks if the specified character is a whitespace character.
     *
     * A character is considered a whitespace if it is one of the following:
     * space (' '), tab ('\t'), carriage return ('\r'), newline ('\n'), or form feed ('\f').
     *
     * @param c the character to check for being a whitespace.
     * @return true if the character is a whitespace, false otherwise.
     */
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f';
    }

    /**
     * Checks if the given character is a newline character.
     *
     * @param c the character to check
     * @return true if the character is a newline character ('\n' or '\r'), false otherwise
     */
    @SuppressWarnings("unused")
    private boolean isNewline(char c) {
        return c == '\n' || c == '\r';
    }

    /**
     * Determines if the given character is a numeric digit (0-9).
     *
     * @param c the character to check
     * @return true if the character is a digit, false otherwise
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if a given character is a valid hexadecimal digit.
     *
     * @param c the character to check
     * @return true if the character is a hexadecimal digit (0-9, a-f, or A-F), false otherwise
     */
    @SuppressWarnings("all")
    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Checks if the given character is an alphabetic character or an underscore.
     *
     * @param c the character to be checked
     * @return true if the character is an alphabetic character ('a' to 'z', 'A' to 'Z') or an underscore ('_'),
     *         false otherwise
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * Determines if the given character is a valid starting character for an identifier.
     *
     * @param c the character to be checked
     * @return true if the character is an alphabetic character or a dollar sign ('$'),
     *         false otherwise
     */
    // TODO: Unicode identifier support (e.g. isIdentStart can include non-ASCII letters, isIdentContinue can include combining marks, etc.)
    // TODO: We can basically make any rules we want for identifiers, as long as the lexer and parser are consistent. For example, we could allow emojis in identifiers if we wanted to. The current rules are just a common baseline.
    private boolean isIdentifierStart(char c) {
        return isAlpha(c) || c == '$';
    }

    /**
     * Determines if the given character is a valid continuation character
     * for an identifier in a programming context. A character is considered
     * valid if it is an alphabetic character, a numeric digit, or a dollar sign ('$').
     *
     * @param c the character to be evaluated
     * @return true if the character can be part of an identifier after the initial character,
     *         otherwise false
     */
    private boolean isIdentifierContinue(char c) {
        return isAlpha(c) || isDigit(c) || c == '$';
    }

    /**
     * Consumes leading trivia (such as whitespace and comments) from the input source
     * and optionally collects it into the provided list of trivia pieces.
     * This method processes trivia based on specified options, such as whether to emit
     * or retain whitespace and comments.
     *
     * @param outLeading The list in which leading trivia pieces should be collected, if applicable.
     *                    If the `isKeepTrivia` option is enabled, trivia items such as whitespace
     *                    and comments will be added to this list as {@code TriviaPiece} objects.
     */
    private void consumeTrivia(ArrayList<TriviaPiece> outLeading) {
        while(!atEnd()) {
            char c = current();

            // Whitespace
            if (isWhitespace(c)) {
                if (getOptions().isEmitWhitespace()) break;

                beginToken();
                while(!atEnd() && isWhitespace(current())) advance();
                if (getOptions().isKeepTrivia()) {
                    TriviaPiece piece = new TriviaPiece(TokenKind.Whitespace, endTokenSpan(), tokenLexeme());
                    outLeading.add(piece);
                }
                continue;
            }

            // Comments
            if (c == '/' && (lookaheadChar(1) == '/' || lookaheadChar(1) == '*')) {
                if (getOptions().isEmitComments()) break;

                beginToken();
                if (lookaheadChar(1) == '/') consumeLineComment();
                else consumeBlockComment();

                if (getOptions().isKeepTrivia()) {
                    TriviaPiece piece = new TriviaPiece(TokenKind.Comment, endTokenSpan(), tokenLexeme());
                    outLeading.add(piece);
                }
                continue;
            }
            break;
        }
    }

    /**
     * Consumes trailing trivia, such as whitespace or comments, and appends it to the provided {@link Trivia} object
     * if the current parsing options allow keeping trivia.
     *
     * @param outTrailing the {@link Trivia} object to store the trailing trivia pieces, such as whitespace or comments.
     *                    It is updated with any trailing trivia encountered during parsing.
     */
    private void maybeConsumeTrailingTrivia(Trivia outTrailing) {
        if (!options.isKeepTrivia()) return;

        while (!atEnd()) {
            char c = current();
            if (c == ' ' || c == '\t' || c == '\u000B' || c == '\f') {
                beginToken();
                while (!atEnd()) {
                    char cc = current();
                    if (cc == ' ' || cc == '\t' || cc == '\u000B' || cc == '\f') advance();
                    else break;
                }
                TriviaPiece tr = new TriviaPiece(TokenKind.Whitespace, endTokenSpan(), tokenLexeme());
                outTrailing.trailing().add(tr);
                continue;
            }

            if (c == '/' && lookaheadChar(1) == '/') {
                beginToken();
                consumeLineComment();
                TriviaPiece tr = new TriviaPiece(TokenKind.Comment, endTokenSpan(), tokenLexeme());
                outTrailing.trailing().add(tr);
                continue;
            }

            break;
        }
    }

    /**
     * Initializes the beginning of a new token by recording the
     * current position in the source being analyzed. Updates the
     * token's starting position, offset, line number, and column
     * based on the current parsing state.
     * <p>
     * This method is typically called at the start of token recognition
     * to ensure that the token's metadata reflects the correct
     * location in the source.
     */
    private void beginToken() {
        tokenBegin = current;
        tokenOffset = offset;
        tokenLine = line;
        tokenColumn = column;
    }

    /**
     * Creates a new SourceSpan object representing the span of the current token.
     * The span is determined using the starting and ending locations of the token
     * based on the file, offset, line, and column values.
     *
     * @return a SourceSpan object that encapsulates the starting and ending
     *         locations of the current token.
     */
    private SourceSpan endTokenSpan() {
        SourceLocation start = new SourceLocation(buffer.getFile(), tokenOffset, tokenLine, tokenColumn);
        SourceLocation end = new SourceLocation(buffer.getFile(), offset, line, column);
        return new SourceSpan(start, end);
    }

    /**
     * Extracts and returns the lexeme from the text buffer based on the current token boundaries.
     * <p>
     * This method calculates the length of the token using the difference between the
     * current position and the token start position. If the calculated length is zero or negative,
     * it returns an empty string. Otherwise, it returns a substring from the text buffer that
     * represents the lexeme.
     *
     * @return the lexeme as a string extracted from the text buffer, or an empty string if the token length is non-positive
     */
    private String tokenLexeme() {
        int len = current - tokenBegin;
        if (len <= 0) return "";
        return new String(text, tokenBegin, len);
    }

    /**
     * Creates and returns a new Token instance based on the provided TokenKind and default token components.
     *
     * @param kind the kind of the token to be created
     * @return a new Token instance with the specified kind and other default attributes
     */
    private Token makeToken(TokenKind kind) {
        return new Token(kind, Tokens.Keyword.None, Tokens.Operator.None, endTokenSpan(), tokenLexeme());
    }

    /**
     * Creates and returns a new Token instance with the specified kind and keyword.
     *
     * @param kind The kind of the token to be created.
     * @param keyword The keyword associated with the token.
     * @return A new Token instance initialized with the provided kind and keyword.
     */
    private Token makeToken(TokenKind kind, Tokens.Keyword keyword) {
        return new Token(kind, keyword, Tokens.Operator.None, endTokenSpan(), tokenLexeme());
    }

    /**
     * Creates a new Token object with the specified kind, operator,
     * and the current token span and lexeme.
     *
     * @param kind The kind of token to create.
     * @param operator The operator associated with the token.
     * @return A new Token object constructed with the provided parameters.
     */
    private Token makeToken(TokenKind kind, Tokens.Operator operator) {
        return new Token(kind, Tokens.Keyword.None, operator, endTokenSpan(), tokenLexeme());
    }

    /**
     * Creates an error token with the specified error message and logs the error information to diagnostics.
     *
     * @param message The error message to associate with the error token.
     * @return The created error token of type {@code TokenKind.Error}.
     */
    private Token makeErrorToken(String message) {
        diagnostics.error(new SourceLocation(buffer.getFile(), tokenOffset, tokenLine, tokenColumn), message);
        return makeToken(TokenKind.Error);
    }

    /**
     * Scans and processes the end of file token.
     *
     * This method signals the end of the input by creating a token
     * for the end of file. It initializes the token processing
     * using the beginToken method and generates the end-of-file
     * token using makeToken.
     *
     * @return the token representing the end of file
     */
    private IToken scanEndOfFile() {
        beginToken();
        return makeToken(TokenKind.EndOfFile);
    }

    /**
     * Scans and returns a token representing consecutive whitespace characters.
     * The method begins token creation, iterates through the input while the current
     * character is a whitespace character, and continues advancing the position
     * until a non-whitespace character or the end of the input is reached.
     *
     * @return a token of kind Whitespace representing the scanned sequence of whitespace characters
     */
    private IToken scanWhitespaceToken() {
        beginToken();
        while (!atEnd() && isWhitespace(current())) advance();
        return makeToken(TokenKind.Whitespace);
    }

    /**
     * Scans the input stream to determine if the current sequence of characters forms an identifier or a keyword.
     * The method starts scanning from the current character position and continues until it finds a valid token boundary.
     * It resolves meta-keywords, standard keywords, or defaults to identifying the sequence as an identifier.
     *
     * @return An {@code IToken} representing either a meta-keyword token, a keyword token, or an identifier token
     * depending on the resolved token type based on the scanned input.
     */
    private IToken scanIdentifierOrKeyword() {
        beginToken();
        do advance();
        while (!atEnd() && isIdentifierContinue(current()));

        String ident = tokenLexeme();
        MetaKeyword metaKeyword = resolveMetaKeyword(ident);
        if (metaKeyword != MetaKeyword.NONE) {
            return new MetaToken(TokenKind.Meta, endTokenSpan(), tokenLexeme(), metaKeyword);
        }

        Tokens.Keyword kw = resolveKeyword(ident);
        return kw != Tokens.Keyword.None ? makeToken(TokenKind.Keyword, kw) : makeToken(TokenKind.Identifier);
    }

    /**
     * Scans and identifies numeric literals in the input, including decimal, hexadecimal, binary,
     * floating-point, and scientific notation formats. This method correctly handles underscores
     * as digit separators and validates appropriate numeric syntax.
     *
     * @return An {@code IToken} representing the scanned numeric literal if valid.
     *         If the numeric literal is malformed, an error token is returned.
     */
    private IToken scanNumber() {
        beginToken();

        if (current() == '0' && (lookaheadChar(1) == 'x' || lookaheadChar(1) == 'X')) {
            advanceN(2);
            boolean any = false;
            while (!atEnd()) {
                char c = current();
                if (c == '_') {
                    advance();
                    continue;
                }
                if (!isHexDigit(c)) break;
                any = true;
                advance();
            }
            if (!any) {
                return makeErrorToken("Malformed hex literal (expected digits after 0x)");
            }
            return makeToken(TokenKind.Number);
        }

        if (current() == '0' && (lookaheadChar(1) == 'b' || lookaheadChar(1) == 'B')) {
            advanceN(2);
            boolean any = false;
            while (!atEnd()) {
                char c = current();
                if (c == '_') {
                    advance();
                    continue;
                }
                if (c != '0' && c != '1') break;
                any = true;
                advance();
            }
            if (!any) {
                return makeErrorToken("Malformed binary literal (expected digits after 0b)");
            }
            return makeToken(TokenKind.Number);
        }

        while (!atEnd()) {
            char c = current();
            if (c == '_') {
                advance();
                continue;
            }
            if (!isDigit(c)) break;
            advance();
        }

        if (current() == '.' && isDigit(lookaheadChar(1))) {
            advance();
            while (!atEnd()) {
                char c = current();
                if (c == '_') {
                    advance();
                    continue;
                }
                if (!isDigit(c)) break;
                advance();
            }
        }

        if (current() == 'e' || current() == 'E') {
            advance();
            if (current() == '+' || current() == '-') advance();
            boolean any = false;
            while (!atEnd()) {
                char c = current();
                if (c == '_') {
                    advance();
                    continue;
                }
                if (!isDigit(c)) break;
                any = true;
                advance();
            }
            if (!any) {
                return makeErrorToken("Malformed exponent in numeric literal");
            }
        }

        if (isAlpha(current())) {
            while (!atEnd()) {
                char c = current();
                if (isAlpha(c) || isDigit(c)) advance();
                else break;
            }
        }

        return makeToken(TokenKind.Number);
    }

    /**
     * Scans and processes a string literal token from the input, starting with the
     * specified quote character. Handles escape sequences and enforces size limits.
     *
     * @param quote the character that marks the beginning and end of the string literal
     * @return the token representing the string literal if successfully scanned, or
     *         an error token if the string literal is invalid or terminates improperly
     */
    private IToken scanStringLiteral(char quote) {
        beginToken();
        advance();

        int bytes = 0;
        while (!atEnd()) {
            char c = current();
            if (c == quote) {
                advance();
                return makeToken(TokenKind.String);
            }

            if (c == '\n') {
                return makeErrorToken("Unterminated string literal");
            }

            advance();
            if (c == '\\') {
                if (!consumeEscapeSequence()) {
                    return makeErrorToken("Invalid escape sequence in string literal");
                }
            }
            bytes += 1;

            if (bytes > options.getMaxStringLiteralBytes()) {
                return makeErrorToken("String literal exceeds maximum size limit");
            }
        }

        return makeErrorToken("Unterminated string literal at end of file");
    }

    /**
     * Scans and processes either a comment (line or block) or a slash operator ('/').
     * Based on the context and configuration, the method identifies the appropriate
     * token to generate.
     * <p>
     * The method differentiates between a single-line comment (//), block comment (/* */
    private IToken scanCommentOrSlashOperator() {
        if (lookaheadChar(1) == '/') {
            if (options.isEmitComments()) {
                beginToken();
                consumeLineComment();
                return makeToken(TokenKind.Comment);
            }

            Trivia dummy = new Trivia();
            consumeTrivia(dummy.leading());
            return lexOne().token();
        }

        if (lookaheadChar(1) == '*') {
            if (options.isEmitComments()) {
                beginToken();
                consumeBlockComment();
                return makeToken(TokenKind.Comment);
            }

            Trivia dummy = new Trivia();
            consumeTrivia(dummy.leading());
            return lexOne().token();
        }

        beginToken();
        advance();
        return makeToken(TokenKind.Operator, Tokens.Operator.Slash);
    }

    /**
     * Scans the input source to detect an operator or punctuation token.
     * It matches multi-character operators (e.g., "++", "==", etc.) first,
     * and if no match is found, it processes single-character operators or punctuation symbols.
     *
     * @return an {@code IToken} representing the recognized operator or punctuation token.
     *         If no valid token is found, it generates an error token.
     */
    private IToken scanOperatorOrPunctuation() {
        beginToken();

        if (matchString("...")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.DotDotDot);
        }

        if (matchString("++")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.PlusPlus);
        }
        if (matchString("--")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.MinusMinus);
        }
        if (matchString("==")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.Equal);
        }
        if (matchString("!=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.NotEqual);
        }
        if (matchString("<=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.LessEqual);
        }
        if (matchString(">=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.GreaterEqual);
        }
        if (matchString("->")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.Arrow);
        }
        if (matchString("+=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.PlusAssign);
        }
        if (matchString("-=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.MinusAssign);
        }
        if (matchString("*=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.StarAssign);
        }
        if (matchString("/=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.SlashAssign);
        }
        if (matchString("%=")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.PercentAssign);
        }
        if (matchString("::")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.ColonColon);
        }
        if (matchString("..")) {
            return makeToken(TokenKind.Operator, Tokens.Operator.DotDot);
        }

        char c = current();
        advance();

        return switch (c) {
            case '+' -> makeToken(TokenKind.Operator, Tokens.Operator.Plus);
            case '-' -> makeToken(TokenKind.Operator, Tokens.Operator.Minus);
            case '*' -> makeToken(TokenKind.Operator, Tokens.Operator.Star);
            case '/' -> makeToken(TokenKind.Operator, Tokens.Operator.Slash);
            case '%' -> makeToken(TokenKind.Operator, Tokens.Operator.Percent);
            case '=' -> makeToken(TokenKind.Operator, Tokens.Operator.Assign);
            case '<' -> makeToken(TokenKind.Operator, Tokens.Operator.Less);
            case '>' -> makeToken(TokenKind.Operator, Tokens.Operator.Greater);
            case '!' -> makeToken(TokenKind.Operator, Tokens.Operator.Bang);
            case '&' -> makeToken(TokenKind.Operator, Tokens.Operator.Amp);
            case '|' -> makeToken(TokenKind.Operator, Tokens.Operator.Pipe);
            case '~' -> makeToken(TokenKind.Operator, Tokens.Operator.Tilde);
            case '^' -> makeToken(TokenKind.Operator, Tokens.Operator.Caret);
            case '(' -> makeToken(TokenKind.Punctuation, Tokens.Operator.LeftParen);
            case ')' -> makeToken(TokenKind.Punctuation, Tokens.Operator.RightParen);
            case '{' -> makeToken(TokenKind.Punctuation, Tokens.Operator.LeftBrace);
            case '}' -> makeToken(TokenKind.Punctuation, Tokens.Operator.RightBrace);
            case '[' -> makeToken(TokenKind.Punctuation, Tokens.Operator.LeftBracket);
            case ']' -> makeToken(TokenKind.Punctuation, Tokens.Operator.RightBracket);
            case '.' -> makeToken(TokenKind.Punctuation, Tokens.Operator.Dot);
            case ',' -> makeToken(TokenKind.Punctuation, Tokens.Operator.Comma);
            case ':' -> makeToken(TokenKind.Punctuation, Tokens.Operator.Colon);
            case ';' -> makeToken(TokenKind.Punctuation, Tokens.Operator.Semicolon);
            case '?' -> makeToken(TokenKind.Operator, Tokens.Operator.Question);
            case '@' -> makeToken(TokenKind.Operator, Tokens.Operator.At);
            case '#' -> makeToken(TokenKind.Operator, Tokens.Operator.Hash);
            case '$' -> makeToken(TokenKind.Operator, Tokens.Operator.Dollar);
            case '`' -> makeToken(TokenKind.Operator, Tokens.Operator.Backtick);
            default -> makeErrorToken("Unexpected character in input");
        };

    }

    /**
     * Retrieves the current location information of the source being parsed or processed.
     *
     * @return a SourceLocation object containing details about the file, offset, line, and column
     *         of the current location in the source.
     */
    private SourceLocation currentLocation() {
        return new SourceLocation(buffer.getFile(), offset, line, column);
    }

    /**
     * Consumes a line comment from the current position in the input if it starts with "//".
     * <p>
     * This method checks if the current position begins with the line comment marker ("//").
     * If so, it advances through the input characters until it reaches the end of the line or
     * the end of the input. The newline character is not consumed.
     * <p>
     * The purpose of this method is to efficiently skip over line comments while parsing input.
     * <p>
     * Method Assumptions:
     * - The method operates on a data source that supports navigation and inspection
     *   of individual characters, such as a string or a character buffer.
     * - Helper methods like matchString(String), atEnd(), current(), and advance() are properly defined
     *   and manage the input navigation.
     */
    private void consumeLineComment() {
        if (!matchString("//")) return;
        while (!atEnd() && current() != '\n') advance();
    }

    /**
     * Processes and consumes a block comment from the input source, supporting nested block comments if enabled.
     *
     * The method checks for the opening sequence of a block comment "/*" to begin consumption. It keeps track
     * of comment nesting levels if nested block comments are allowed. The block comment is considered completed
     * when the corresponding closing sequence is present.
     */
    private void consumeBlockComment() {
        if (!matchString("/*")) return;

        int depth = 1;
        while (!atEnd()) {
            if (matchString("*/")) {
                depth--;
                if (depth == 0) return;
                continue;
            }

            if (kNestedBlockComments) {
                if (matchString("/*")) {
                    depth++;
                    continue;
                }
            }

            advance();
        }

        diagnostics.error(new SourceLocation(buffer.getFile(), tokenOffset, tokenLine, tokenColumn), "Unterminated block comment");
    }

    /**
     * Consumes and validates an escape sequence in the current context.
     * Checks if the sequence starting at the current position corresponds
     * to a valid escape sequence such as a character escape (e.g., \n, \t),
     * hexadecimal escape (\xNN), or Unicode escape.
     * Advances the position accordingly if the sequence is valid.
     *
     * @return {@code true} if a valid escape sequence is successfully consumed,
     *         {@code false} otherwise.
     */
    private boolean consumeEscapeSequence() {
        if (atEnd()) return false;
        char c = current();
        advance();

        switch (c) {
            case 'n':
            case 'r':
            case 't':
            case '\\':
            case '\'':
            case '"':
            case '0':
                return true;
            case 'x':
                if (!isHexDigit(current())) return false;
                advance();
                if (!isHexDigit(current())) return false;
                advance();
                return true;
            case 'u':
                for (int i = 0; i < 4; ++i) {
                    if (!isHexDigit(current())) return false;
                    advance();
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Resolves a given identifier string to its corresponding keyword representation
     * in the `Tokens.Keyword` enum, if applicable. If the identifier does not match
     * any known keyword, {@code Tokens.Keyword.None} is returned.
     *
     * @param ident the identifier string to be resolved; may be {@code null} or empty.
     * @return the resolved keyword as a {@code Tokens.Keyword} enum value, or
     *         {@code Tokens.Keyword.None} if the identifier does not match any keyword.
     */
    private Tokens.Keyword resolveKeyword(String ident) {
        if (ident == null || ident.isEmpty()) return Tokens.Keyword.None;
        switch (ident.charAt(0)) {
            case 'a':
                switch (ident) {
                    case "abstract" -> {
                        return Tokens.Keyword.Abstract;
                    }
                    case "absolute" -> {
                        return Tokens.Keyword.Absolute;
                    }
                    case "as" -> {
                        return Tokens.Keyword.As;
                    }
                    case "async" -> {
                        return Tokens.Keyword.Async;
                    }
                    case "await" -> {
                        return Tokens.Keyword.Await;
                    }
                    case "atomic" -> {
                        return Tokens.Keyword.Atomic;
                    }
                    case "and" -> {
                        return Tokens.Keyword.And;
                    }
                    case "any" -> {
                        return Tokens.Keyword.Any;
                    }
                }
                break;
            case 'b':
                switch (ident) {
                    case "break" -> {
                        return Tokens.Keyword.Break;
                    }
                    case "bool" -> {
                        return Tokens.Keyword.Bool;
                    }
                    case "byte" -> {
                        return Tokens.Keyword.Byte;
                    }
                    case "bit" -> {
                        return Tokens.Keyword.Bit;
                    }
                }
                break;
            case 'c':
                switch (ident) {
                    case "const" -> {
                        return Tokens.Keyword.Const;
                    }
                    case "continue" -> {
                        return Tokens.Keyword.Continue;
                    }
                    case "class" -> {
                        return Tokens.Keyword.Class;
                    }
                    case "case" -> {
                        return Tokens.Keyword.Case;
                    }
                    case "char" -> {
                        return Tokens.Keyword.Char;
                    }
                    case "catch" -> {
                        return Tokens.Keyword.Catch;
                    }
                }
                break;
            case 'd':
                switch (ident) {
                    case "defer" -> {
                        return Tokens.Keyword.Defer;
                    }
                    case "delete" -> {
                        return Tokens.Keyword.Delete;
                    }
                    case "do" -> {
                        return Tokens.Keyword.Do;
                    }
                    case "default" -> {
                        return Tokens.Keyword.Default;
                    }
                    case "double" -> {
                        return Tokens.Keyword.F64;
                    }
                }
                break;
            case 'e':
                switch (ident) {
                    case "else" -> {
                        return Tokens.Keyword.Else;
                    }
                    case "enum" -> {
                        return Tokens.Keyword.Enum;
                    }
                }
                break;
            case 'f':
                switch (ident) {
                    case "for" -> {
                        return Tokens.Keyword.For;
                    }
                    case "func" -> {
                        return Tokens.Keyword.Func;
                    }
                    case "float", "f32" -> {
                        return Tokens.Keyword.F32;
                    }
                    case "f64" -> {
                        return Tokens.Keyword.F64;
                    }
                    case "finally" -> {
                        return Tokens.Keyword.Finally;
                    }
                    case "false" -> {
                        return Tokens.Keyword.False;
                    }
                    case "final" -> {
                        return Tokens.Keyword.Final;
                    }
                }
                break;
            case 'i':
                switch (ident) {
                    case "interface" -> {
                        return Tokens.Keyword.Interface;
                    }
                    case "internal" -> {
                        return Tokens.Keyword.Internal;
                    }
                    case "import" -> {
                        return Tokens.Keyword.Import;
                    }
                    case "int", "i32" -> {
                        return Tokens.Keyword.I32;
                    }
                    case "i8" -> {
                        return Tokens.Keyword.I8;
                    }
                    case "i16" -> {
                        return Tokens.Keyword.I16;
                    }
                    case "i64" -> {
                        return Tokens.Keyword.I64;
                    }
                    case "if" -> {
                        return Tokens.Keyword.If;
                    }
                    case "in" -> {
                        return Tokens.Keyword.In;
                    }
                    case "is" -> {
                        return Tokens.Keyword.Is;
                    }
                }
                break;
            case 'l':
                if (ident.equals("let")) return Tokens.Keyword.Let;
                if (ident.equals("long")) return Tokens.Keyword.I64;
                break;
            case 'm':
                if (ident.equals("module")) return Tokens.Keyword.Module;
                break;
            case 'n':
                if (ident.equals("null")) return Tokens.Keyword.Null;
                if (ident.equals("new")) return Tokens.Keyword.New;
                break;
            case 'o':
                switch (ident) {
                    case "or" -> {
                        return Tokens.Keyword.Or;
                    }
                    case "owned" -> {
                        return Tokens.Keyword.Owned;
                    }
                    case "override" -> {
                        return Tokens.Keyword.Override;
                    }
                }
                break;
            case 'p':
                if (ident.equals("public")) return Tokens.Keyword.Public;
                if (ident.equals("private")) return Tokens.Keyword.Private;
                break;
            case 'r':
                switch (ident) {
                    case "return" -> {
                        return Tokens.Keyword.Return;
                    }
                    case "real" -> {
                        return Tokens.Keyword.F64;
                    }
                    case "ref" -> {
                        return Tokens.Keyword.Ref;
                    }
                }
                break;
            case 's':
                switch (ident) {
                    case "struct" -> {
                        return Tokens.Keyword.Struct;
                    }
                    case "switch" -> {
                        return Tokens.Keyword.Switch;
                    }
                    case "string" -> {
                        return Tokens.Keyword.String;
                    }
                    case "super" -> {
                        return Tokens.Keyword.Super;
                    }
                    case "short" -> {
                        return Tokens.Keyword.I16;
                    }
                    case "shared" -> {
                        return Tokens.Keyword.Shared;
                    }
                    case "static" -> {
                        return Tokens.Keyword.Static;
                    }
                }
                break;
            case 't':
                switch (ident) {
                    case "this" -> {
                        return Tokens.Keyword.This;
                    }
                    case "throw" -> {
                        return Tokens.Keyword.Throw;
                    }
                    case "try" -> {
                        return Tokens.Keyword.Try;
                    }
                    case "true" -> {
                        return Tokens.Keyword.True;
                    }
                }
                break;
            case 'u':
                switch (ident) {
                    case "uint", "u32" -> {
                        return Tokens.Keyword.U32;
                    }
                    case "u8" -> {
                        return Tokens.Keyword.U8;
                    }
                    case "u16" -> {
                        return Tokens.Keyword.U16;
                    }
                    case "u64" -> {
                        return Tokens.Keyword.U64;
                    }
                }
                break;
            case 'v':
                if (ident.equals("var")) return Tokens.Keyword.Var;
                if (ident.equals("void")) return Tokens.Keyword.Void;
                break;
            case 'w':
                if (ident.equals("while")) return Tokens.Keyword.While;
                break;
            default:
                break;
        }
        return Tokens.Keyword.None;
    }

    /**
     * Resolves the provided identifier string to a corresponding {@code MetaKeyword} enumeration value.
     * If the identifier is null, empty, or does not match any predefined keywords,
     * it returns {@code MetaKeyword.NONE}.
     *
     * @param ident the identifier string to be resolved into a {@code MetaKeyword}
     * @return the resolved {@code MetaKeyword} value corresponding to the provided identifier;
     *         returns {@code MetaKeyword.NONE} if the identifier is invalid or unrecognized
     */
    private MetaKeyword resolveMetaKeyword(String ident) {
        if (ident == null || ident.isEmpty()) return MetaKeyword.NONE;
        return switch (ident) {
            case "ALIGNOF" -> MetaKeyword.ALIGNOF;
            case "DEFAULT" -> MetaKeyword.DEFAULT;
            case "LENGTH" -> MetaKeyword.LENGTH;
            case "MAX" -> MetaKeyword.MAX;
            case "MEMSPACE" -> MetaKeyword.MEMSPACE;
            case "MIN" -> MetaKeyword.MIN;
            case "SIZEOF" -> MetaKeyword.SIZEOF;
            case "TO_BITS" -> MetaKeyword.TO_BITS;
            case "TO_BYTES" -> MetaKeyword.TO_BYTES;
            case "TO_STRING" -> MetaKeyword.TO_STRING;
            case "TYPEOF" -> MetaKeyword.TYPEOF;
            default -> MetaKeyword.NONE;
        };
    }

    /**
     * Updates the location markers (line, column, and offset) based on the consumed character.
     *
     * @param consumed the character that was processed, which determines how the
     *                 line and column values should be updated. A newline character ('\n')
     *                 increments the line counter and resets the column counter to 1.
     *                 Any other character increments the column counter.
     */
    private void bumpLocation(char consumed) {
        offset = current;
        if (consumed == '\n') {
            line += 1;
            column = 1;
        } else {
            column += 1;
        }
    }

}
