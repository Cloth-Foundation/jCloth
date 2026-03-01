package cloth.parser;

import cloth.error.Error;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.flags.DeclarationFlags;
import cloth.parser.flags.Visibility;
import cloth.token.IToken;
import cloth.token.Token;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an abstract base class for parsing components of a language's syntax. Provides utility
 * methods to interact with the lexer, handle tokens, and process specific language constructs.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 * @param <T> The return type for parsed structures.
 */
public abstract class ParserPart<T> implements Parsable<T> {

    @Getter
    private final Lexer lexer;

    @Getter
    private final SourceFile file;

    public ParserPart(Lexer lexer, SourceFile file) {
        this.lexer = lexer;
        this.file = file;
    }

    /**
     * Retrieves the next token in the source code without consuming it.
     * This method provides a lookahead capability to inspect the upcoming
     * token for parsing decisions while leaving the current position
     * in the token stream unchanged.
     *
     * @return the next {@code IToken} instance in the token stream.
     */
    public IToken peek() {
        return getLexer().peek(0).token();
    }

    /**
     * Retrieves a token from the lookahead token stream at a specified offset,
     * without consuming it. This method provides the ability to inspect a token
     * at a relative position while preserving the current state of the lexer,
     * facilitating lookahead capabilities during parsing.
     *
     * @param offset the zero-based offset indicating the position of the token
     *               to inspect, relative to the current position in the token stream.
     *               Must be a non-negative integer.
     * @return the {@code IToken} instance at the specified offset in the token stream.
     */
    public IToken peek(int offset) {
        return getLexer().peek(offset).token();
    }

    /**
     * Retrieves the token immediately preceding the current position in the token stream.
     * This allows access to the previously parsed token without altering the current
     * state of the lexer or advancing the token stream.
     *
     * @return the {@code IToken} instance representing the preceding token in the token stream.
     */
    public IToken previous() {
        return getLexer().getPreviousToken();
    }

    /**
     * Determines if the end of the file has been reached by checking if the lexer
     * has no more tokens to process. This method helps identify whether the
     * parsing process has consumed all input.
     *
     * @return true if the lexer indicates that it has reached the end of the file, false otherwise.
     */
    public boolean isEndOfFile() {
        return getLexer().eof();
    }

    /**
     * Determines whether the next token in the source code matches the specified {@code TokenKind}.
     * This method provides a quick way to check the type of the upcoming token without consuming it.
     *
     * @param kind the {@code TokenKind} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code TokenKind}; {@code false} otherwise.
     */
    public boolean is(TokenKind kind) {
        return peek().is(kind);
    }

    /**
     * Determines if the next token in the token stream matches the specified {@code Tokens.Keyword}.
     * This method checks both the token kind and the keyword type to perform a precise match.
     *
     * @param keyword the {@code Tokens.Keyword} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code Tokens.Keyword}; {@code false} otherwise.
     */
    public boolean is(Tokens.Keyword keyword) {
        IToken token = peek();
        return token.is(TokenKind.Keyword) && ((Token) token).keyword() == keyword;
    }

    /**
     * Determines if the next token in the token stream matches the specified {@code Tokens.Operator}.
     * This method checks whether the next token is either an operator or a punctuation token
     * and compares it to the provided operator.
     *
     * @param operator the {@code Tokens.Operator} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code Tokens.Operator};
     *         {@code false} otherwise.
     */
    public boolean is(Tokens.Operator operator) {
        IToken token = peek();
        if (token.is(TokenKind.Operator) || token.is(TokenKind.Punctuation)) {
            return ((Token) token).operator() == operator;
        }
        return false;
    }

    /**
     * Matches the next token in the token stream against the specified {@code TokenKind}.
     * If the next token matches, the token is consumed and the method returns {@code true};
     * otherwise, it returns {@code false}.
     *
     * @param kind the {@code TokenKind} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code TokenKind} and is consumed;
     *         {@code false} otherwise.
     */
    public boolean match(TokenKind kind) {
        if (is(kind)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Matches the next token in the token stream against the specified {@code Tokens.Keyword}.
     * If the next token matches, the token is consumed and the method returns {@code true};
     * otherwise, it returns {@code false}.
     *
     * @param keyword the {@code Tokens.Keyword} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code Tokens.Keyword} and is consumed;
     *         {@code false} otherwise.
     */
    public boolean match(Tokens.Keyword keyword) {
        if (is(keyword)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Matches the next token in the token stream against the specified {@code Tokens.Operator}.
     * If the next token matches, the token is consumed and the method returns {@code true};
     * otherwise, it returns {@code false}.
     *
     * @param operator the {@code Tokens.Operator} to compare against the next token.
     * @return {@code true} if the next token matches the specified {@code Tokens.Operator}
     *         and is consumed; {@code false} otherwise.
     */
    public boolean match(Tokens.Operator operator) {
        if (is(operator)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Advances the lexer to consume the next token in the token stream.
     * This method retrieves the next token from the lexer while updating
     * the internal state to reflect the consumed token.
     *
     * @return the {@code IToken} instance representing the new current token
     *         in the token stream after advancing.
     */
    public IToken advance() {
        return getLexer().next().token();
    }

    /**
     * Ensures that the next token in the token stream matches the specified {@code TokenKind}.
     * If the token matches, the method advances the token stream and returns the matched token.
     * Otherwise, it throws the error provided by the given {@code Supplier<Error>}.
     *
     * @param kind the {@code TokenKind} to match against the next token in the stream.
     * @param error a {@code Supplier<Error>} that provides the error to throw if the token does not match.
     * @return the {@code IToken} instance representing the matched token after advancing the stream.
     * @throws java.lang.Error if the next token does not match the specified {@code TokenKind}.
     */
    @SneakyThrows
    public IToken expect(TokenKind kind, @NotNull Supplier<Error> error) {
        if (is(kind)) return advance();
        throw error.get();
    }

    /**
     * Ensures that the next token in the token stream matches the specified {@code Tokens.Keyword}.
     * If the next token matches, it advances the token stream and returns the matched token.
     * Otherwise, it throws the error provided by the given {@code Supplier<Error>}.
     *
     * @param keyword the {@code Tokens.Keyword} to match against the next token in the stream.
     * @param error a {@code Supplier<Error>} that provides the error to throw if the token does not match.
     * @return the {@code IToken} instance representing the matched token after advancing the stream.
     * @throws java.lang.Error if the next token does not match the specified {@code Tokens.Keyword}.
     */
    @SneakyThrows
    public IToken expect(Tokens.Keyword keyword, @NotNull Supplier<Error> error) {
        if (is(keyword)) return advance();
        throw error.get();
    }

    /**
     * Verifies that the current token matches the expected operator. If the token matches,
     * it advances to the next token and returns the matched token. If the token does not match,
     * it throws an error provided by the supplied error generator.
     *
     * @param operator the operator to check against the current token
     * @param error a supplier function that provides the exception to be thrown if the check fails
     * @return the matched token if the operator matches the current token
     */
    @SneakyThrows
    public IToken expect(Tokens.Operator operator, @NotNull Supplier<Error> error) {
        if (is(operator)) return advance();
        throw error.get();
    }

    /**
     * Ensures that the next token in the lexer stream is a semicolon.
     * If the semicolon is not present, it throws a CompileError indicating
     * that a semicolon was expected and provides suggestions to rectify the issue.
     *
     * @return The semicolon token if it is present in the lexer stream.
     */
    public IToken expectSemiColon() {
        return expect(Tokens.Operator.Semicolon, () -> new CompileError("Expected ';'", getLexer().getPreviousToken().span(), "Insert a semicolon at the end of the statement.", "Statements and expressions end with a semicolon."));
    }

    /**
     * Consumes modifier keywords from the token stream and accumulates them into a
     * {@link DeclarationFlags} instance. Handles visibility ({@code public}, {@code private},
     * {@code internal}), {@code static}, {@code final}, {@code abstract}, and {@code override}.
     * <p>
     * Validates syntactic constraints:
     * <ul>
     *   <li>No duplicate modifiers (e.g. {@code static static})</li>
     *   <li>No conflicting visibility (e.g. {@code public private})</li>
     *   <li>No conflicting modifiers (e.g. {@code abstract final})</li>
     * </ul>
     */
    @SneakyThrows
    public DeclarationFlags parseDeclarationFlags() {
        var flags = new DeclarationFlags();
        while (true) {
            if (is(Tokens.Keyword.Public)) {
                rejectDuplicateVisibility(flags);
                flags.setVisibility(Visibility.Type.PUBLIC);
                flags.setVisibilityToken(advance());
            } else if (is(Tokens.Keyword.Private)) {
                rejectDuplicateVisibility(flags);
                flags.setVisibility(Visibility.Type.PRIVATE);
                flags.setVisibilityToken(advance());
            } else if (is(Tokens.Keyword.Internal)) {
                rejectDuplicateVisibility(flags);
                flags.setVisibility(Visibility.Type.INTERNAL);
                flags.setVisibilityToken(advance());
            } else if (is(Tokens.Keyword.Static)) {
                rejectDuplicateModifier(flags.isStatic(), Tokens.Keyword.Static.toString());
                flags.setStatic(true);
                flags.setStaticToken(advance());
            } else if (is(Tokens.Keyword.Final)) {
                rejectDuplicateModifier(flags.isFinal(), Tokens.Keyword.Final.toString());
                flags.setFinal(true);
                flags.setFinalToken(advance());
            } else if (is(Tokens.Keyword.Abstract)) {
                rejectDuplicateModifier(flags.isAbstract(), Tokens.Keyword.Abstract.toString());
                flags.setAbstract(true);
                flags.setAbstractToken(advance());
            } else if (is(Tokens.Keyword.Override)) {
                rejectDuplicateModifier(flags.isOverride(), Tokens.Keyword.Override.toString());
                flags.setOverride(true);
                flags.setOverrideToken(advance());
            } else {
                break;
            }
        }

        if (flags.isAbstract() && flags.isFinal()) {
            throw new CompileError(
                "'abstract' and 'final' cannot be combined",
                flags.getAbstractToken().span(),
                "A declaration cannot be both abstract and final.",
                "'abstract' means it must be overridden; 'final' means it cannot be."
            );
        }

        return flags;
    }

    /**
     * Validates that a declaration does not contain duplicate or conflicting
     * visibility modifiers. Throws a {@code CompileError} if duplicate or
     * contradictory visibility modifiers are found.
     *
     * @param flags The {@code DeclarationFlags} object that holds information
     *              about the current declaration, including its visibility
     *              modifier.
     * @throws CompileError if duplicate or conflicting visibility modifiers
     *                       are detected in the declaration.
     */
    @SneakyThrows
    private void rejectDuplicateVisibility(DeclarationFlags flags) {
        if (flags.getVisibility() != null) {
            String existing = flags.getVisibility().name().toLowerCase();
            String incoming = peek().lexeme();
            if (existing.equals(incoming)) {
                throw new CompileError(
                    "Duplicate modifier '" + existing + "'",
                    peek().span(),
                    "Remove the duplicate modifier.",
                    "Each modifier may only appear once."
                );
            } else {
                throw new CompileError(
                    "Conflicting visibility modifiers '" + existing + "' and '" + incoming + "'",
                    peek().span(),
                    "A declaration may have at most one visibility modifier.",
                    "Choose one of: public, private, or internal."
                );
            }
        }
    }

    /**
     * Rejects a duplicate modifier by throwing a {@code CompileError} if the modifier
     * has already been set.
     *
     * @param alreadySet A boolean indicating if the modifier has already been set.
     *                   If {@code true}, an exception will be thrown.
     * @param name       The name of the modifier that is being checked for duplication.
     *                   This will be included in the exception message if a duplicate is found.
     * @throws CompileError If the modifier is already set, this exception is thrown with
     *                      details to help identify and resolve the issue.
     */
    @SneakyThrows
    private void rejectDuplicateModifier(boolean alreadySet, String name) {
        if (alreadySet) {
            throw new CompileError(
                "Duplicate modifier '" + name + "'",
                peek().span(),
                "Remove the duplicate modifier.",
                "Each modifier may only appear once."
            );
        }
    }

    /**
     * Peeks the next declaration keyword from the token stream, skipping over any
     * storage modifiers or other modifiers if present.
     *
     * @return The next declaration keyword if one is found, or {@code Tokens.Keyword.None}
     *         if no declaration keyword is identified.
     */
    public Tokens.Keyword peekDeclarationKeyword() {
        int offset = 0;
        while (true) {
            var token = peek(offset);
            if (token.is(TokenKind.Keyword) && ((Token) token).keyword().isStorageModifier()) {
                offset++;
            } else if (token.is(TokenKind.Keyword) && ((Token) token).keyword().isModifier()) {
                offset++;
            } else {
                if (token.is(TokenKind.Keyword)) {
                    return ((Token) token).keyword();
                }
                return Tokens.Keyword.None;
            }
        }
    }

}
