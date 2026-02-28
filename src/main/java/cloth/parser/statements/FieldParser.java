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

/**
 * Parses a field or local variable declaration of the form:
 * <pre>[modifiers] binding name: Type [= expr];</pre>
 * Where {@code binding} is one of {@code var}, {@code let}, or {@code const}.
 * Returns {@code null} if the current token is not a binding keyword,
 * allowing callers to try other parsers.
 */
public class FieldParser extends ParserPart<FieldParser.FieldDeclaration> {

    public FieldParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public @Nullable FieldDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        BindingKind binding;
        IToken bindingToken;
        if (is(Tokens.Keyword.Var)) {
            binding = BindingKind.VAR;
            bindingToken = advance();
        } else if (is(Tokens.Keyword.Let)) {
            binding = BindingKind.LET;
            bindingToken = advance();
        } else if (is(Tokens.Keyword.Const)) {
            binding = BindingKind.CONST;
            bindingToken = advance();
        } else {
            if (flags.hasFlags()) {
                throw new CompileError(
                    "Expected a declaration after modifiers",
                    peek().span(),
                    "Modifiers must be followed by 'var', 'let', 'const', 'class', 'func', etc.",
                    "public var x: i32 = 0;"
                );
            }
            return null;
        }

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected field name", peek().span(),
                "Expected an identifier for the field name.",
                "var name: Type = value;"));

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':'", peek().span(),
                "Expected ':' between field name and type.",
                "var name: Type;"));

        TypeReferenceParser.TypeReference type =
            new TypeReferenceParser(getLexer(), getFile()).parse();

        Expression initializer = null;
        if (is(Tokens.Operator.Assign)) {
            advance(); // consume =
            initializer = new ExpressionParser(getLexer(), getFile()).parse();
        }

        IToken semicolon = expectSemiColon();

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : bindingToken.span().start(),
            semicolon.span().end()
        );

        return new FieldDeclaration(flags, binding, bindingToken, name, type, initializer, span);
    }

    // region Records

    public enum BindingKind {
        VAR, LET, CONST
    }

    public record FieldDeclaration(
        DeclarationFlags flags,
        BindingKind binding,
        IToken bindingToken,
        IToken name,
        TypeReferenceParser.TypeReference type,
        @Nullable Expression initializer,
        SourceSpan span
    ) {}

    // endregion
}
