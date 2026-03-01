package cloth.parser;

import cloth.error.Error;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.flags.DeclarationFlags;
import cloth.parser.flags.Visibility;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import lombok.Getter;
import lombok.SneakyThrows;
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

    public IToken peek() {
        return getLexer().peek(0).token();
    }

    public IToken peek(int offset) {
        return getLexer().peek(offset).token();
    }

    public IToken previous() {
        return getLexer().getPreviousToken();
    }

    public boolean isEndOfFile() {
        return getLexer().eof();
    }

    public boolean is(TokenKind kind) {
        return peek().is(kind);
    }

    public boolean is(Tokens.Keyword keyword) {
        IToken token = peek();
        return token.is(TokenKind.Keyword) && ((cloth.token.Token) token).keyword() == keyword;
    }

    public boolean is(Tokens.Operator operator) {
        IToken token = peek();
        if (token.is(TokenKind.Operator) || token.is(TokenKind.Punctuation)) {
            return ((cloth.token.Token) token).operator() == operator;
        }
        return false;
    }

    public boolean match(TokenKind kind) {
        if (is(kind)) {
            advance();
            return true;
        }
        return false;
    }

    public boolean match(Tokens.Keyword keyword) {
        if (is(keyword)) {
            advance();
            return true;
        }
        return false;
    }

    public boolean match(Tokens.Operator operator) {
        if (is(operator)) {
            advance();
            return true;
        }
        return false;
    }

    public IToken advance() {
        return getLexer().next().token();
    }

    @SneakyThrows
    public IToken expect(TokenKind kind, @NotNull java.util.function.Supplier<Error> error) {
        if (is(kind)) return advance();
        throw error.get();
    }

    @SneakyThrows
    public IToken expect(Tokens.Keyword keyword, @NotNull java.util.function.Supplier<Error> error) {
        if (is(keyword)) return advance();
        throw error.get();
    }

    @SneakyThrows
    public IToken expect(Tokens.Operator operator, @NotNull java.util.function.Supplier<Error> error) {
        if (is(operator)) return advance();
        throw error.get();
    }

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
     * Peeks ahead past any modifier keywords to find the actual declaration keyword
     * (e.g. {@code class}, {@code func}, {@code var}). Does not consume any tokens.
     */
    public Tokens.Keyword peekDeclarationKeyword() {
        int offset = 0;
        while (true) {
            var token = peek(offset);
            if (token.is(TokenKind.Keyword) && ((cloth.token.Token) token).keyword().isStorageModifier()) {
                offset++;
            } else if (token.is(TokenKind.Keyword) && ((cloth.token.Token) token).keyword().isModifier()) {
                offset++;
            } else {
                if (token.is(TokenKind.Keyword)) {
                    return ((cloth.token.Token) token).keyword();
                }
                return Tokens.Keyword.None;
            }
        }
    }

}
