package cloth.parser;

import cloth.error.Error;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

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

}
