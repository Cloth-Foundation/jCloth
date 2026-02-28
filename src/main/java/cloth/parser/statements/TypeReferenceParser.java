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

/**
 * Parses a type reference of the form {@code Type}, {@code Type?}, {@code Type[]},
 * or any combination such as {@code Type?[][]}. Reusable across parameter lists,
 * field declarations, method return types, and any other context that expects a type.
 */
public class TypeReferenceParser extends ParserPart<TypeReferenceParser.TypeReference> {

    public TypeReferenceParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public TypeReference parse() {
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

    public record TypeReference(
        IToken baseName,
        boolean nullable,
        int arrayDepth,
        SourceSpan span
    ) {}
}
