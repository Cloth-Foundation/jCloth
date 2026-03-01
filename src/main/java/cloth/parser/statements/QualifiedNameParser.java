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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser component specialized in processing and extracting qualified names
 * from a lexical token stream.
 * <p>
 * Qualified names are hierarchical identifiers made up of dot-separated
 * segments. Each segment can be an identifier or a reserved keyword, and the
 * entire name is validated during parsing.
 * <p>
 * The {@code QualifiedNameParser} extends the functionality of the
 * {@code ParserPart} base class and ensures that qualified names conform to
 * expected language syntax. Errors encountered during parsing are reported
 * with detailed information, including the location and nature of the issue.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *  <li>Parsing tokens into valid qualified names.</li>
 *  <li>Validating and ensuring that each segment conforms to rules for identifiers or keywords.</li>
 *  <li>Building a structured representation of a qualified name for use in further code analysis or compilation steps.</li>
 * </ul>
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *  <li>Delineates segments of a qualified name and associates them with their source span for error reporting or other analysis.</li>
 *  <li>Supports hierarchical names in the format "segment1.segment2.segment3".</li>
 * </ul>
 * <p>
 * <strong>General Structure:</strong>
 * <ul>
 *  <li>A {@code QualifiedNameParser} takes a {@link Lexer} instance and {@link SourceFile} as input, allowing it to analyze the corresponding token stream.</li>
 *  <li>The parsing process results in a {@link QualifiedName} object, which contains the segments, source span, and separator.</li>
 * </ul>
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class QualifiedNameParser extends ParserPart<QualifiedNameParser.QualifiedName> {

    public QualifiedNameParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    /**
     * Parses a qualified name from the token stream.
     * A qualified name consists of one or more dot-separated identifiers or keywords.
     * <p>
     * This method processes the tokens in the source code, extracting a sequence of
     * segments representing a qualified name. Each segment is validated to ensure
     * it is either an identifier or a keyword. If parsing fails at any point,
     * an exception is thrown with detailed error information.
     *
     * @return a {@code QualifiedName} object representing the fully parsed and validated qualified name,
     *         including its segments, source span, and separator.
     */
    @SneakyThrows
    @Override
    public QualifiedName parse() {
        IToken first;
        if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
            first = advance();
        } else {
            throw new CompileError("Expected identifier", peek().span(), "Expected an identifier for the qualified name.", "A qualified name is a dot-separated sequence of identifiers.");
        }

        var parts = new ArrayList<IToken>();
        parts.add(first);

        while (match(Tokens.Operator.Dot)) {
            IToken part;
            if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
                part = advance();
            } else {
                throw new CompileError("Expected identifier", peek().span(), "Expected an identifier after '.'.", "A qualified name is a dot-separated sequence of identifiers.");
            }
            parts.add(part);
        }

        var segments = parts.stream().map(IToken::lexeme).toList();
        SourceSpan span = new SourceSpan(first.span().start(), parts.getLast().span().end());
        return new QualifiedName(segments, span, Tokens.Operator.Dot.toString());
    }

    /**
     * Represents a fully qualified name made up of multiple segments.
     * <p>
     * This class provides a way to represent and manipulate hierarchical
     * names, commonly used in programming contexts such as package names,
     * file paths, or scoped identifiers.
     * <p>
     * Components:
     * <li>The qualified name is stored as a list of string segments, where each segment represents part of the hierarchy.</li>
     * <li>A {@link SourceSpan} is associated with the qualified name to indicate its location in the source code.</li>
     * <p>
     * Features:
     * <li>The {@code toString()} method joins all segments with a dot ('.') to produce a string representation of the full qualified name.</li>
     */
    public record QualifiedName(List<String> segments, SourceSpan span, String seperator) {

        @Override
        public @NotNull String toString() {
            return String.join(seperator, segments);
        }

    }
}
