package cloth.error;

import cloth.error.errors.CompileError;
import cloth.error.errors.DeclarationError;
import cloth.error.errors.ModifierError;
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
 * a default {@code label} (shown next to the underline), an optional
 * {@code fix} string that is spliced into the actual source line to produce
 * a context-aware help hint, and a {@code defaultBuilder} that determines the
 * concrete error type produced by the convenience {@code toError()} overloads.
 * <p>
 * Entries whose message or label contains {@code %s} placeholders are
 * <em>parameterized</em> — use {@link #toFormattedError} instead of
 * {@link #toError} so the placeholders are filled in.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum CommonErrors {

    // ── Syntax (default: SyntaxError) ────────────────────────────────

    // Punctuation
    EXPECTED_SEMICOLON("Expected ';'", "Missing semicolon.", Tokens.Operator.Semicolon, InsertPosition.AFTER_SPAN, SyntaxError::new),
    EXPECTED_COLON("Expected ':'", "Expected ':' separator.", Tokens.Operator.Colon, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_OPEN_BRACE("Expected '{'", "Expected opening brace.", Tokens.Operator.LeftBrace, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_CLOSE_BRACE("Expected '}'", "Expected closing brace.", Tokens.Operator.RightBrace, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_OPEN_PAREN("Expected '('", "Expected opening parenthesis.", Tokens.Operator.LeftParen, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_CLOSE_PAREN("Expected ')'", "Expected closing parenthesis.", Tokens.Operator.RightParen, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_CLOSE_BRACKET("Expected ']'", "Expected closing bracket.", Tokens.Operator.RightBracket, InsertPosition.AT_SPAN, SyntaxError::new),

    // Names / expressions / types
    EXPECTED_IDENTIFIER("Expected identifier", "Expected a name.", TokenKind.Identifier, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_EXPRESSION("Expected expression", "Expected an expression.", "", InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_TYPE("Expected type", "Expected a type.", TokenKind.Keyword, InsertPosition.AT_SPAN, SyntaxError::new),

    // Declaration keywords
    EXPECTED_KEYWORD_CLASS("Expected 'class'", "Expected 'class' keyword.", Tokens.Keyword.Class, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_KEYWORD_FUNC("Expected 'func'", "Expected 'func' keyword.", Tokens.Keyword.Func, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_KEYWORD_ENUM("Expected 'enum'", "Expected 'enum' keyword.", Tokens.Keyword.Enum, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_KEYWORD_STRUCT("Expected 'struct'", "Expected 'struct' keyword.", Tokens.Keyword.Struct, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_KEYWORD_INTERFACE("Expected 'interface'", "Expected 'interface' keyword.", Tokens.Keyword.Interface, InsertPosition.AT_SPAN, SyntaxError::new),
    EXPECTED_KEYWORD_MODULE("Expected 'module'", "Expected 'module' keyword.", Tokens.Keyword.Module, InsertPosition.AT_SPAN, SyntaxError::new),

    // ── Modifiers (default: ModifierError) ───────────────────────────

    /**
     * Parameterized: {@code toFormattedError(span, modifierName)}
     */
    DUPLICATE_MODIFIER("Duplicate modifier '%s'", "Remove the duplicate modifier.", ModifierError::new),

    /**
     * Parameterized: {@code toFormattedError(span, existing, incoming)}
     */
    CONFLICTING_VISIBILITY("Conflicting visibility modifiers '%s' and '%s'", "A declaration may have at most one visibility modifier.", ModifierError::new),

    ABSTRACT_FINAL_CONFLICT("'abstract' and 'final' cannot be combined", "A declaration cannot be both abstract and final.", ModifierError::new),

    /**
     * Parameterized: {@code toFormattedError(span, modifierName, declarationKind)}
     */
    INVALID_MODIFIER("'%s' is not valid on %s", "Remove the '%s' modifier.", ModifierError::new),

    // ── Declarations (default: DeclarationError) ─────────────────────

    ABSTRACT_METHOD_HAS_BODY("Abstract methods must not have a body", "Remove the method body or the 'abstract' modifier.", DeclarationError::new),

    METHOD_MISSING_BODY("Non-abstract methods must have a body", "Add a method body or mark the method 'abstract'.", DeclarationError::new),

    INTERFACE_NO_FIELDS("Interfaces cannot declare fields", "Remove the field declaration.", DeclarationError::new),

    INTERFACE_NO_METHOD_BODY("Interface methods must not have a body", "Remove the method body. Interface methods are signatures only.", DeclarationError::new),

    DEFER_REQUIRES_CALL("Expected call expression after 'defer'", "Only call expressions may be deferred.", DeclarationError::new),
    ;

    public enum InsertPosition {AT_SPAN, AFTER_SPAN}

    private static final Ansi FIX_STYLE = Ansi.Red.and(Ansi.Bold);

    @Getter
    private final String message;

    @Getter
    private final String label;

    @Getter
    private final @Nullable String fix;
    private final InsertPosition insertPosition;
    private final ErrorBuilder<? extends CompileError> defaultBuilder;

    // ── Canonical constructor ────────────────────────────────────────

    CommonErrors(String message, String label, @Nullable String fix, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this.message = message;
        this.label = label;
        this.fix = fix;
        this.insertPosition = insertPosition;
        this.defaultBuilder = defaultBuilder;
    }

    // ── No-fix constructor (modifier / declaration entries) ────────────

    CommonErrors(String message, String label,ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, (String) null, InsertPosition.AT_SPAN, defaultBuilder);
    }

    // ── Convenience constructors (token-to-fix-string) ───────────────

    CommonErrors(String message, String label, Tokens.Operator operator, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, operator.toString(), insertPosition, defaultBuilder);
    }

    CommonErrors(String message, String label, Tokens.Keyword keyword, InsertPosition insertPosition,ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, keyword.toString(), insertPosition, defaultBuilder);
    }

    CommonErrors(String message, String label, TokenKind kind, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, kind.toString(), insertPosition, defaultBuilder);
    }

    // ── toError (fixed-message entries) ──────────────────────────────

    /**
     * Builds an error using the entry's default type and default label.
     */
    public CompileError toError(SourceSpan span) {
        return defaultBuilder.build(message, span, label, buildHelp(span));
    }

    /**
     * Builds an error using the entry's default type with a custom label.
     */
    public CompileError toError(SourceSpan span, String labelOverride) {
        return defaultBuilder.build(message, span, labelOverride, buildHelp(span));
    }

    /**
     * Builds an error of caller-chosen type {@code E} with the default label.
     */
    public <E extends CompileError> E toError(SourceSpan span, ErrorBuilder<E> builder) {
        return builder.build(message, span, label, buildHelp(span));
    }

    /**
     * Builds an error of caller-chosen type {@code E} with a custom label.
     */
    public <E extends CompileError> E toError(SourceSpan span, String labelOverride, ErrorBuilder<E> builder) {
        return builder.build(message, span, labelOverride, buildHelp(span));
    }

    // ── toFormattedError (parameterized entries) ─────────────────────

    /**
     * Formats both the message and the label with the given arguments, then
     * builds an error using the entry's default type.
     * <p>
     * Use this for entries whose {@code message} / {@code label} contain
     * {@code %s} placeholders (e.g. {@code DUPLICATE_MODIFIER},
     * {@code INVALID_MODIFIER}).
     */
    public CompileError toFormattedError(SourceSpan span, Object... args) {
        String fmtMessage = String.format(message, args);
        String fmtLabel = String.format(label, args);
        return defaultBuilder.build(fmtMessage, span, fmtLabel, fix != null ? buildHelp(span) : fmtLabel);
    }

    /**
     * Formats the message with the given arguments and uses a fixed label
     * that is <em>not</em> run through {@code String.format}.
     * <p>
     * Named differently from {@link #toFormattedError(SourceSpan, Object...)}
     * to avoid varargs ambiguity when the first argument is a {@code String}.
     */
    public CompileError toFormattedErrorWithLabel(SourceSpan span, String labelOverride, Object... args) {
        String fmtMessage = String.format(message, args);
        return defaultBuilder.build(fmtMessage, span, labelOverride, fix != null ? buildHelp(span) : labelOverride);
    }

    /**
     * Formats the message with the given arguments and builds an error of
     * caller-chosen type {@code E}.
     */
    public <E extends CompileError> E toFormattedError(SourceSpan span, ErrorBuilder<E> builder, Object... args) {
        String fmtMessage = String.format(message, args);
        String fmtLabel = String.format(label, args);
        return builder.build(fmtMessage, span, fmtLabel, fix != null ? buildHelp(span) : fmtLabel);
    }

    // ── Help-string builder ──────────────────────────────────────────

    /**
     * Reads the source line from the span and inserts the {@code fix} at the
     * appropriate column, highlighted with ANSI codes.
     *
     * @return the annotated line, or the label as fallback
     */
    private @Nullable String buildHelp(SourceSpan span) {
        if (fix == null || span == null || !span.isValid()) return label;
        try {
            String source = span.start().file().getSourceText();
            String line = getLine(source, span.start().line());
            if (line == null) return label;

            int col = (insertPosition == InsertPosition.AFTER_SPAN) ? span.end().column() - 1 : span.start().column() - 1;

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
