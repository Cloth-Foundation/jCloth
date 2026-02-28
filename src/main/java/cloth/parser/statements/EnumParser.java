package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.expressions.Expression;
import cloth.parser.expressions.ExpressionParser;
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
 * Parses an enum declaration of the form:
 * <pre>[modifiers] enum Name [(params)] { Case1, Case2(args), Case3 = 42; members... }</pre>
 * <p>
 * When a primary constructor is present, each case provides positional constructor
 * arguments. When absent, cases may declare data payloads ({@code name: Type}).
 */
public class EnumParser extends ParserPart<EnumParser.EnumDeclaration> {

    private boolean hasPrimaryConstructor;

    public EnumParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public EnumDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        IToken enumKeyword = expect(Tokens.Keyword.Enum, () ->
            new CompileError("Expected 'enum'", peek().span(),
                "Expected an enum declaration.",
                "Enum declarations begin with the 'enum' keyword."));

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected enum name", peek().span(),
                "An enum name must be a valid identifier.",
                "enum Color { Red, Green, Blue }"));

        List<ParameterListParser.Parameter> primaryConstructor = null;
        if (is(Tokens.Operator.LeftParen)) {
            primaryConstructor = new ParameterListParser(getLexer(), getFile()).parse();
            hasPrimaryConstructor = true;
        }

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for enum body.",
                "enum Color { Red, Green, Blue }"));

        List<EnumCase> cases = parseCases();

        var fields = new ArrayList<FieldParser.FieldDeclaration>();
        var methods = new ArrayList<FuncParser.FuncDeclaration>();
        if (match(Tokens.Operator.Semicolon)) {
            parseEnumMembers(fields, methods);
        }

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for enum body.",
                "enum Color { Red, Green, Blue }"));

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : enumKeyword.span().start(),
            closeBrace.span().end()
        );

        return new EnumDeclaration(flags, name, primaryConstructor, cases, fields, methods, span);
    }

    // region Cases

    @SneakyThrows
    private List<EnumCase> parseCases() {
        var cases = new ArrayList<EnumCase>();

        if (is(Tokens.Operator.RightBrace) || is(Tokens.Operator.Semicolon)) {
            return cases;
        }

        cases.add(parseCase());
        while (match(Tokens.Operator.Comma)) {
            if (is(Tokens.Operator.RightBrace) || is(Tokens.Operator.Semicolon)) {
                break; // trailing comma
            }
            cases.add(parseCase());
        }

        return cases;
    }

    @SneakyThrows
    private EnumCase parseCase() {
        IToken caseName = expect(TokenKind.Identifier, () ->
            new CompileError("Expected case name", peek().span(),
                "Enum case names must be valid identifiers.",
                "enum Color { Red, Green, Blue }"));

        List<ParameterListParser.Parameter> payload = null;
        List<Expression> constructorArgs = null;

        if (is(Tokens.Operator.LeftParen)) {
            if (hasPrimaryConstructor) {
                constructorArgs = parseConstructorArgs();
            } else {
                payload = new ParameterListParser(getLexer(), getFile()).parse();
            }
        }

        Expression discriminant = null;
        if (is(Tokens.Operator.Assign)) {
            advance(); // consume =
            discriminant = new ExpressionParser(getLexer(), getFile()).parse();
        }

        IToken last = caseName;
        if (discriminant != null) {
            last = null; // span end from discriminant expression
        } else if (constructorArgs != null || (payload != null && !payload.isEmpty())) {
            last = previous();
        }

        SourceSpan span = new SourceSpan(
            caseName.span().start(),
            discriminant != null ? discriminant.span().end() : last.span().end()
        );
        return new EnumCase(caseName, payload, constructorArgs, discriminant, span);
    }

    @SneakyThrows
    private List<Expression> parseConstructorArgs() {
        expect(Tokens.Operator.LeftParen, () ->
            new CompileError("Expected '('", peek().span(),
                "Expected constructor arguments.",
                "CaseName(arg1, arg2)"));

        var args = new ArrayList<Expression>();
        if (!is(Tokens.Operator.RightParen)) {
            args.add(new ExpressionParser(getLexer(), getFile()).parse());
            while (match(Tokens.Operator.Comma)) {
                args.add(new ExpressionParser(getLexer(), getFile()).parse());
            }
        }

        expect(Tokens.Operator.RightParen, () ->
            new CompileError("Expected ')'", peek().span(),
                "Expected closing parenthesis for constructor arguments.",
                "CaseName(arg1, arg2)"));

        return args;
    }

    // endregion

    // region Members

    /**
     * Parses optional members after the semicolon: fields (and eventually methods).
     * Uses the same dispatch pattern as {@link ClassParser}.
     */
    private void parseEnumMembers(List<FieldParser.FieldDeclaration> fields,
                                  List<FuncParser.FuncDeclaration> methods) {
        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) {
            Tokens.Keyword memberKeyword = peekDeclarationKeyword();

            if (memberKeyword == Tokens.Keyword.Var
                || memberKeyword == Tokens.Keyword.Let
                || memberKeyword == Tokens.Keyword.Const) {
                fields.add(new FieldParser(getLexer(), getFile()).parse());
            } else if (memberKeyword == Tokens.Keyword.Func) {
                methods.add(new FuncParser(getLexer(), getFile()).parse());
            } else {
                skipUnknownMember();
            }
        }
    }

    private void skipUnknownMember() {
        if (is(Tokens.Operator.LeftBrace)) {
            advance();
            int depth = 1;
            while (depth > 0 && !isEndOfFile()) {
                if (is(Tokens.Operator.LeftBrace)) depth++;
                else if (is(Tokens.Operator.RightBrace)) depth--;
                if (depth > 0) advance();
            }
            if (!isEndOfFile()) advance();
        } else {
            advance();
        }
    }

    // endregion

    // region Records

    public record EnumDeclaration(
        DeclarationFlags flags,
        IToken name,
        @Nullable List<ParameterListParser.Parameter> primaryConstructor,
        List<EnumCase> cases,
        List<FieldParser.FieldDeclaration> fields,
        List<FuncParser.FuncDeclaration> methods,
        SourceSpan span
    ) {}

    public record EnumCase(
        IToken name,
        @Nullable List<ParameterListParser.Parameter> payload,
        @Nullable List<Expression> constructorArgs,
        @Nullable Expression discriminant,
        SourceSpan span
    ) {}

    // endregion
}
