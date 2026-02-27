package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
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

        TypeReference type = parseTypeReference();

        List<IToken> defaultValue = null;
        if (is(Tokens.Operator.Assign)) {
            defaultValue = parseDefaultValue();
        }

        SourceSpan span = new SourceSpan(
            name.span().start(),
            defaultValue != null && !defaultValue.isEmpty()
                ? defaultValue.getLast().span().end()
                : type.span().end()
        );

        return new Parameter(name, type, defaultValue, span);
    }

    // endregion

    // region Type Reference

    @SneakyThrows
    private TypeReference parseTypeReference() {
        IToken baseName;
        if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
            baseName = advance();
        } else {
            throw new CompileError("Expected type", peek().span(),
                "Expected a type name.",
                "name: Type");
        }

        boolean nullable = false;
        int arrayDepth = 0;
        IToken lastToken = baseName;

        while (true) {
            if (is(Tokens.Operator.Question)) {
                nullable = true;
                lastToken = advance();
            } else if (is(Tokens.Operator.LeftBracket)) {
                advance();
                lastToken = expect(Tokens.Operator.RightBracket, () ->
                    new CompileError("Expected ']'", peek().span(),
                        "Expected closing bracket for array type.",
                        "Type[]"));
                arrayDepth++;
            } else {
                break;
            }
        }

        SourceSpan span = new SourceSpan(baseName.span().start(), lastToken.span().end());
        return new TypeReference(baseName, nullable, arrayDepth, span);
    }

    // endregion

    // region Default Value

    /**
     * Consumes tokens for a default value expression after {@code =}.
     * Collects raw tokens until an unbalanced {@code ,} or {@code )} is reached,
     * respecting nested parentheses, brackets, and braces.
     */
    private List<IToken> parseDefaultValue() {
        advance(); // consume =

        var tokens = new ArrayList<IToken>();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;

        while (!isEndOfFile()) {
            if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                if (is(Tokens.Operator.Comma) || is(Tokens.Operator.RightParen)) {
                    break;
                }
            }

            if (is(Tokens.Operator.LeftParen)) parenDepth++;
            else if (is(Tokens.Operator.RightParen)) parenDepth--;
            else if (is(Tokens.Operator.LeftBracket)) bracketDepth++;
            else if (is(Tokens.Operator.RightBracket)) bracketDepth--;
            else if (is(Tokens.Operator.LeftBrace)) braceDepth++;
            else if (is(Tokens.Operator.RightBrace)) braceDepth--;

            tokens.add(advance());
        }

        return tokens;
    }

    // endregion

    // region Records

    public record Parameter(
        IToken name,
        TypeReference type,
        @Nullable List<IToken> defaultValue,
        SourceSpan span
    ) {}

    public record TypeReference(
        IToken baseName,
        boolean nullable,
        int arrayDepth,
        SourceSpan span
    ) {}

    // endregion
}
