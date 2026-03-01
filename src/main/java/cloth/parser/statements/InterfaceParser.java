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

import java.util.ArrayList;
import java.util.List;

/**
 * Parses an interface declaration of the form:
 * <pre>[visibility] interface Name { method_signatures }</pre>
 * In v1, interface members are limited to method signatures (no fields, no default methods).
 * Interfaces may only carry a visibility modifier; {@code static}, {@code final},
 * {@code abstract}, and {@code override} are rejected.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class InterfaceParser extends ParserPart<InterfaceParser.InterfaceDeclaration> {

    public InterfaceParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public InterfaceDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        rejectModifier(flags.isStatic(), flags.getStaticToken(), "static");
        rejectModifier(flags.isFinal(), flags.getFinalToken(), "final");
        rejectModifier(flags.isAbstract(), flags.getAbstractToken(), "abstract");
        rejectModifier(flags.isOverride(), flags.getOverrideToken(), "override");

        IToken interfaceKeyword = expect(Tokens.Keyword.Interface, () ->
            new CompileError("Expected 'interface'", peek().span(),
                "Expected an interface declaration.",
                "Interface declarations begin with the 'interface' keyword."));

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected interface name", peek().span(),
                "An interface name must be a valid identifier.",
                "interface Drawable { }"));

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for interface body.",
                "interface Drawable { func draw(): void; }"));

        List<MethodSignature> methods = parseInterfaceBody();

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for interface body.",
                "interface Drawable { func draw(): void; }"));

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : interfaceKeyword.span().start(),
            closeBrace.span().end()
        );

        return new InterfaceDeclaration(flags, name, methods, span);
    }

    // region Interface Body

    @SneakyThrows
    private List<MethodSignature> parseInterfaceBody() {
        var methods = new ArrayList<MethodSignature>();

        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) {
            Tokens.Keyword memberKw = peekDeclarationKeyword();

            if (memberKw == Tokens.Keyword.Var
                || memberKw == Tokens.Keyword.Let
                || memberKw == Tokens.Keyword.Const) {
                throw new CompileError(
                    "Interfaces cannot declare fields",
                    peek().span(),
                    "Remove the field declaration.",
                    "Interface members must be method signatures."
                );
            }

            methods.add(parseMethodSignature());
        }

        return methods;
    }

    @SneakyThrows
    private MethodSignature parseMethodSignature() {
        IToken funcKeyword = expect(Tokens.Keyword.Func, () ->
            new CompileError("Expected method signature", peek().span(),
                "Interface members must be method signatures starting with 'func'.",
                "func draw(): void;"));

        IToken methodName = expect(TokenKind.Identifier, () ->
            new CompileError("Expected method name", peek().span(),
                "A method name must be a valid identifier.",
                "func draw(): void;"));

        List<ParameterListParser.Parameter> parameters = new ParameterListParser(getLexer(), getFile()).parse();

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':' after parameters", peek().span(),
                "Interface methods must specify a return type.",
                "func draw(): void;"));

        TypeReferenceParser.TypeReference returnType = new TypeReferenceParser(getLexer(), getFile()).parse();

        if (is(Tokens.Operator.LeftBrace)) {
            throw new CompileError(
                "Interface methods must not have a body",
                peek().span(),
                "Remove the method body. Interface methods are signatures only.",
                "func draw(): void;"
            );
        }

        IToken semi = expectSemiColon();

        SourceSpan span = new SourceSpan(funcKeyword.span().start(), semi.span().end());
        return new MethodSignature(methodName, parameters, returnType, span);
    }

    // endregion

    // region Helpers

    @SneakyThrows
    private void rejectModifier(boolean present, IToken token, String name) {
        if (present) {
            throw new CompileError(
                "'" + name + "' is not valid on an interface",
                token.span(),
                "Remove the '" + name + "' modifier.",
                "Interfaces may only have a visibility modifier (public, private, internal)."
            );
        }
    }

    // endregion

    // region Records

    public record InterfaceDeclaration(
        DeclarationFlags flags,
        IToken name,
        List<MethodSignature> methods,
        SourceSpan span
    ) {}

    public record MethodSignature(
        IToken name,
        List<ParameterListParser.Parameter> parameters,
        TypeReferenceParser.TypeReference returnType,
        SourceSpan span
    ) {}

    // endregion
}
