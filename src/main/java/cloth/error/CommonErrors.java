package cloth.error;

import cloth.error.errors.SyntaxError;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import cloth.utility.Ansi;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Catalogue of frequently-emitted compile errors.
 * <p>
 * Each variant carries a fixed {@code message} (shown in the error header),
 * a default {@code label} (shown next to the underline), and an optional
 * {@code fix} string that is spliced into the actual source line to produce
 * a context-aware help hint.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum CommonErrors {

    // Punctuation
    EXPECTED_SEMICOLON("Expected ';'", "Missing semicolon.", Tokens.Operator.Semicolon, InsertPosition.AFTER_SPAN),
    EXPECTED_COLON("Expected ':'", "Expected ':' separator.", Tokens.Operator.Colon, InsertPosition.AT_SPAN),
    EXPECTED_OPEN_BRACE("Expected '{'", "Expected opening brace.", Tokens.Operator.LeftBrace, InsertPosition.AT_SPAN),
    EXPECTED_CLOSE_BRACE("Expected '}'", "Expected closing brace.", Tokens.Operator.RightBrace, InsertPosition.AT_SPAN),
    EXPECTED_OPEN_PAREN("Expected '('", "Expected opening parenthesis.", Tokens.Operator.LeftParen, InsertPosition.AT_SPAN),
    EXPECTED_CLOSE_PAREN("Expected ')'", "Expected closing parenthesis.", Tokens.Operator.RightParen, InsertPosition.AT_SPAN),
    EXPECTED_CLOSE_BRACKET("Expected ']'", "Expected closing bracket.", Tokens.Operator.RightBracket, InsertPosition.AT_SPAN),

    // Names / expressions / types
    EXPECTED_IDENTIFIER("Expected identifier", "Expected a name.", TokenKind.Identifier, InsertPosition.AT_SPAN),
    EXPECTED_EXPRESSION("Expected expression", "Expected an expression.", "", InsertPosition.AT_SPAN),
    EXPECTED_TYPE("Expected type", "Expected a type.", TokenKind.Keyword, InsertPosition.AT_SPAN),

    // Declaration keywords
    EXPECTED_KEYWORD_CLASS("Expected 'class'", "Expected 'class' keyword.", Tokens.Keyword.Class, InsertPosition.AT_SPAN),
    EXPECTED_KEYWORD_FUNC("Expected 'func'", "Expected 'func' keyword.", Tokens.Keyword.Func, InsertPosition.AT_SPAN),
    EXPECTED_KEYWORD_ENUM("Expected 'enum'", "Expected 'enum' keyword.", Tokens.Keyword.Enum, InsertPosition.AT_SPAN),
    EXPECTED_KEYWORD_STRUCT("Expected 'struct'", "Expected 'struct' keyword.", Tokens.Keyword.Struct, InsertPosition.AT_SPAN),
    EXPECTED_KEYWORD_INTERFACE("Expected 'interface'", "Expected 'interface' keyword.", Tokens.Keyword.Interface, InsertPosition.AT_SPAN),
    EXPECTED_KEYWORD_MODULE("Expected 'module'", "Expected 'module' keyword.", Tokens.Keyword.Module, InsertPosition.AT_SPAN),
    ;

    public enum InsertPosition { AT_SPAN, AFTER_SPAN }

    private static final Ansi FIX_STYLE = Ansi.Red.and(Ansi.Bold);

    @Getter private final String message;
    @Getter private final String label;
    @Getter private final @Nullable String fix;
    private final InsertPosition insertPosition;

    CommonErrors(String message, String label, @Nullable String fix, InsertPosition insertPosition) {
        this.message = message;
        this.label = label;
        this.fix = fix;
        this.insertPosition = insertPosition;
    }

    CommonErrors(String message, String label, Tokens.Operator operator, InsertPosition insertPosition) {
        this(message, label, operator.toString(), insertPosition);
    }

    CommonErrors(String message, String label, Tokens.Keyword keyword, InsertPosition insertPosition) {
        this(message, label, keyword.toString(), insertPosition);
    }

    CommonErrors(String message, String label, TokenKind kind, InsertPosition insertPosition) {
        this(message, label, kind.toString(), insertPosition);
    }

    /**
     * Builds a {@link SyntaxError} using the default label and a smart help string.
     */
    public SyntaxError toError(SourceSpan span) {
        return new SyntaxError(message, span, label, buildHelp(span));
    }

    /**
     * Builds a {@link SyntaxError} with a custom label override.
     */
    public SyntaxError toError(SourceSpan span, String labelOverride) {
        return new SyntaxError(message, span, labelOverride, buildHelp(span));
    }

    /**
     * Reads the source line from the span and inserts the {@code fix} at the
     * appropriate column, highlighted with green + underline ANSI codes.
     *
     * @return the annotated line, or {@code null} if no fix is available
     */
    private @Nullable String buildHelp(SourceSpan span) {
        if (fix == null || span == null || !span.isValid()) return label;
        try {
            String source = span.start().file().getSourceText();
            String line = getLine(source, span.start().line());
            if (line == null) return label;

            int col = (insertPosition == InsertPosition.AFTER_SPAN)
                ? span.end().column() - 1
                : span.start().column() - 1;

            col = Math.max(0, Math.min(col, line.length()));

            String before = line.substring(0, col);
            String after = line.substring(col);
            return before + FIX_STYLE.colorize(fix) + after;
        } catch (Exception e) {
            return label;
        }
    }

    private static @Nullable String getLine(String source, int oneBased) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        int idx = oneBased - 1;
        if (idx < 0 || idx >= lines.length) return null;
        return lines[idx];
    }
}
