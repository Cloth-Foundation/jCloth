package cloth.parser.statements;

import cloth.error.CommonErrors;
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
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class TypeReferenceParser extends ParserPart<TypeReferenceParser.TypeReference> {

    public TypeReferenceParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    /**
     * Parses a type reference from the source code, supporting both nullable and
     * array types. This method processes a base type, followed by optional nullability
     * (indicated by {@code ?}) and array dimensions (indicated by {@code []}).
     *
     * @return a {@code TypeReference} instance representing the parsed type, including
     *         its base name, nullability, array depth, and source span.
     * @throws CompileError if the type reference is invalid or syntactically incorrect.
     */
    @Override
    @SneakyThrows
    public TypeReference parse() {
        IToken baseName;
        if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
            baseName = advance();
        } else {
            throw CommonErrors.EXPECTED_TYPE.toError(peek().span());
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
                lastToken = expect(Tokens.Operator.RightBracket, CommonErrors.EXPECTED_CLOSE_BRACKET);
                arrayDepth++;
            } else {
                break;
            }
        }

        SourceSpan span = new SourceSpan(baseName.span().start(), lastToken.span().end());
        return new TypeReference(baseName, nullable, arrayDepth, span);
    }

    /**
     * Represents a type reference in the source code, defining its base type name,
     * nullability, array dimensions, and source span. A type reference is used
     * in various contexts where type specifications are required, such as variable
     * declarations, method return types, and parameters.
     *
     * @param baseName   The base type name represented as a token.
     * @param nullable   Indicates whether the type is nullable (e.g., {@code Type?}).
     * @param arrayDepth The number of array dimensions (e.g., {@code Type[][]} has an array depth of 2).
     * @param span       The source span covering the type reference in the code.
     */
    public record TypeReference(IToken baseName, boolean nullable, int arrayDepth, SourceSpan span) {}
}
