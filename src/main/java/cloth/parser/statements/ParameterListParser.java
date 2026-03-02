package cloth.parser.statements;

import cloth.error.CommonErrors;
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
        expect(Tokens.Operator.LeftParen, CommonErrors.EXPECTED_OPEN_PAREN);

        var params = new ArrayList<Parameter>();

        if (!is(Tokens.Operator.RightParen)) {
            params.add(parseParameter());
            while (match(Tokens.Operator.Comma)) {
                params.add(parseParameter());
            }
        }

        expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);

        return params;
    }

    // region Parameter

    @SneakyThrows
    private Parameter parseParameter() {
        IToken name = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected parameter name.");

        expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Expected ':' between parameter name and type.");

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
