package cloth.parser.statements;

import cloth.error.CommonErrors;
import cloth.error.errors.DeclarationError;
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

import java.util.List;

/**
 * Parses a method/function declaration of the form:
 * <pre>[modifiers] func name(params): ReturnType { body }</pre>
 * Abstract methods omit the body and terminate with a semicolon:
 * <pre>abstract func name(params): ReturnType;</pre>
 * The body is parsed into a {@link Statement.Block} via {@link StatementParser}.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class FuncParser extends ParserPart<FuncParser.FuncDeclaration> {

    public FuncParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public FuncDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        IToken funcKeyword = expect(Tokens.Keyword.Func, CommonErrors.EXPECTED_KEYWORD_FUNC);
        IToken name = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected function name.");
        List<ParameterListParser.Parameter> parameters = new ParameterListParser(getLexer(), getFile()).parse();
        expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Expected return type after parameters.");
        TypeReferenceParser.TypeReference returnType = new TypeReferenceParser(getLexer(), getFile()).parse();
        Statement.Block body = null;
        IToken last;

        if (flags.isAbstract()) {
            if (is(Tokens.Operator.LeftBrace)) {
                throw new DeclarationError(
                    "Abstract methods must not have a body",
                    peek().span(),
                    "Remove the method body or the 'abstract' modifier.",
                    "abstract func compute(): i32;"
                );
            }
            last = expectSemiColon();
        } else {
            if (is(Tokens.Operator.Semicolon)) {
                throw new DeclarationError(
                    "Non-abstract methods must have a body",
                    peek().span(),
                    "Add a method body or mark the method 'abstract'.",
                    "func compute(): i32 { return 42; }"
                );
            }

            expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);

            body = new StatementParser(getLexer(), getFile()).parseBlock();

            last = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);
        }

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : funcKeyword.span().start(),
            last.span().end()
        );

        return new FuncDeclaration(flags, name, parameters, returnType, body, span);
    }

    // region Records

    public record FuncDeclaration(
        DeclarationFlags flags,
        IToken name,
        List<ParameterListParser.Parameter> parameters,
        TypeReferenceParser.TypeReference returnType,
        @Nullable Statement.Block body,
        SourceSpan span
    ) {}

    // endregion
}
