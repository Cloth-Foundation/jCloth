package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.expressions.Expression;
import cloth.parser.expressions.ExpressionParser;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a parenthesized, comma-separated parameter list of the form:
 * <pre>( name: Type, name: Type? = default, ... )</pre>
 * Reusable across primary constructors, method declarations, and any other
 * context that requires a parameter list.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ParameterListParser extends ParserPart<List<ParameterListParser.Parameter>> {

    public ParameterListParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public List<Parameter> parse() {
        expect(Tokens.Operator.LeftParen, () ->
            new CompileError("Expected '('", peek().span(),
                "Expected opening parenthesis for parameter list.",
                "(name: Type, ...)"));

        var params = new ArrayList<Parameter>();

        if (!is(Tokens.Operator.RightParen)) {
            params.add(parseParameter());
            while (match(Tokens.Operator.Comma)) {
                params.add(parseParameter());
            }
        }

        expect(Tokens.Operator.RightParen, () ->
            new CompileError("Expected ')'", peek().span(),
                "Expected closing parenthesis for parameter list.",
                "(name: Type, ...)"));

        return params;
    }

    // region Parameter

    @SneakyThrows
    private Parameter parseParameter() {
        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected parameter name", peek().span(),
                "Expected an identifier for the parameter name.",
                "name: Type"));

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':'", peek().span(),
                "Expected ':' between parameter name and type.",
                "name: Type"));

        TypeReferenceParser.TypeReference type = parseTypeReference();

        Expression defaultValue = null;
        if (is(Tokens.Operator.Assign)) {
            advance(); // consume =
            defaultValue = new ExpressionParser(getLexer(), getFile()).parse();
        }

        SourceSpan span = new SourceSpan(
            name.span().start(),
            defaultValue != null
                ? defaultValue.span().end()
                : type.span().end()
        );

        return new Parameter(name, type, defaultValue, span);
    }

    // endregion

    // region Type Reference

    private TypeReferenceParser.TypeReference parseTypeReference() {
        return new TypeReferenceParser(getLexer(), getFile()).parse();
    }

    // endregion

    // region Records

    public record Parameter(
        IToken name,
        TypeReferenceParser.TypeReference type,
        @Nullable Expression defaultValue,
        SourceSpan span
    ) {}

    // endregion
}
