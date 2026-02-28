package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.flags.DeclarationFlags;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a method/function declaration of the form:
 * <pre>[modifiers] func name(params): ReturnType { body }</pre>
 * Abstract methods omit the body and terminate with a semicolon:
 * <pre>abstract func name(params): ReturnType;</pre>
 * The body is captured as raw tokens; full statement/expression parsing is deferred.
 */
public class FuncParser extends ParserPart<FuncParser.FuncDeclaration> {

    public FuncParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public FuncDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        IToken funcKeyword = expect(Tokens.Keyword.Func, () ->
            new CompileError("Expected 'func'", peek().span(),
                "Expected a function declaration.",
                "Function declarations begin with the 'func' keyword."));

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected function name", peek().span(),
                "A function name must be a valid identifier.",
                "func doSomething(): void { }"));

        List<ParameterListParser.Parameter> parameters =
            new ParameterListParser(getLexer(), getFile()).parse();

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':' after parameters", peek().span(),
                "Functions must specify a return type after the parameter list.",
                "func add(a: i32, b: i32): i32 { ... }"));

        TypeReferenceParser.TypeReference returnType =
            new TypeReferenceParser(getLexer(), getFile()).parse();

        List<IToken> body = null;
        IToken last;

        if (flags.isAbstract()) {
            if (is(Tokens.Operator.LeftBrace)) {
                throw new CompileError(
                    "Abstract methods must not have a body",
                    peek().span(),
                    "Remove the method body or the 'abstract' modifier.",
                    "abstract func compute(): i32;"
                );
            }
            last = expectSemiColon();
        } else {
            if (is(Tokens.Operator.Semicolon)) {
                throw new CompileError(
                    "Non-abstract methods must have a body",
                    peek().span(),
                    "Add a method body or mark the method 'abstract'.",
                    "func compute(): i32 { return 42; }"
                );
            }

            expect(Tokens.Operator.LeftBrace, () ->
                new CompileError("Expected '{'", peek().span(),
                    "Expected opening brace for function body.",
                    "func add(a: i32, b: i32): i32 { return a + b; }"));

            body = parseBody();

            last = expect(Tokens.Operator.RightBrace, () ->
                new CompileError("Expected '}'", peek().span(),
                    "Expected closing brace for function body.",
                    "func add(a: i32, b: i32): i32 { return a + b; }"));
        }

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : funcKeyword.span().start(),
            last.span().end()
        );

        return new FuncDeclaration(flags, name, parameters, returnType, body, span);
    }

    // region Body

    /**
     * Collects raw tokens inside a brace-delimited block, respecting nested
     * brace pairs. The opening {@code &#123;} must already be consumed; the
     * closing {@code &#125;} is left unconsumed for the caller to expect.
     */
    private List<IToken> parseBody() {
        var tokens = new ArrayList<IToken>();
        int depth = 1;

        while (depth > 0 && !isEndOfFile()) {
            if (is(Tokens.Operator.LeftBrace)) depth++;
            else if (is(Tokens.Operator.RightBrace)) depth--;

            if (depth > 0) {
                tokens.add(advance());
            }
        }

        return tokens;
    }

    // endregion

    // region Records

    public record FuncDeclaration(
        DeclarationFlags flags,
        IToken name,
        List<ParameterListParser.Parameter> parameters,
        TypeReferenceParser.TypeReference returnType,
        @Nullable List<IToken> body,
        SourceSpan span
    ) {}

    // endregion
}
