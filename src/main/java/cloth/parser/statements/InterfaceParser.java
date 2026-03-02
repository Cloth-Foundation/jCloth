package cloth.parser.statements;

import cloth.error.CommonErrors;
import cloth.error.errors.DeclarationError;
import cloth.error.errors.ModifierError;
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

        IToken interfaceKeyword = expect(Tokens.Keyword.Interface, CommonErrors.EXPECTED_KEYWORD_INTERFACE);
        IToken name = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected interface name.");
        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);
        List<MethodSignature> methods = parseInterfaceBody();
        IToken closeBrace = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

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
                throw new DeclarationError(
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
        IToken funcKeyword = expect(Tokens.Keyword.Func, CommonErrors.EXPECTED_KEYWORD_FUNC, "Interface members must be method signatures.");

        IToken methodName = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected method name.");

        List<ParameterListParser.Parameter> parameters = new ParameterListParser(getLexer(), getFile()).parse();

        expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Interface methods must specify a return type.");

        TypeReferenceParser.TypeReference returnType = new TypeReferenceParser(getLexer(), getFile()).parse();

        if (is(Tokens.Operator.LeftBrace)) {
            throw new DeclarationError(
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
            throw new ModifierError(
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
