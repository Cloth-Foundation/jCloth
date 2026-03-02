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
 * Catalogue of frequently emitted compile errors.
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

    // Modifiers
    DUPLICATE_MODIFIER("Duplicate modifier '%s'", "Remove the duplicate modifier.", ModifierError::new),
    CONFLICTING_VISIBILITY("Conflicting visibility modifiers '%s' and '%s'", "A declaration may have at most one visibility modifier.", ModifierError::new),
    ABSTRACT_FINAL_CONFLICT("'abstract' and 'final' cannot be combined", "A declaration cannot be both abstract and final.", ModifierError::new),
    INVALID_MODIFIER("'%s' is not valid on %s", "Remove the '%s' modifier.", ModifierError::new),

    // Declarations
    ABSTRACT_METHOD_HAS_BODY("Abstract methods must not have a body", "Remove the method body or the 'abstract' modifier.", DeclarationError::new),
    METHOD_MISSING_BODY("Non-abstract methods must have a body", "Add a method body or mark the method 'abstract'.", DeclarationError::new),
    INTERFACE_NO_FIELDS("Interfaces cannot declare fields", "Remove the field declaration.", DeclarationError::new),
    INTERFACE_NO_METHOD_BODY("Interface methods must not have a body", "Remove the method body. Interface methods are signatures only.", DeclarationError::new),
    DEFER_REQUIRES_CALL("Expected call expression after 'defer'", "Only call expressions may be deferred.", DeclarationError::new),
    ;

    public enum InsertPosition {
        AT_SPAN,
        AFTER_SPAN
    }

    private static final Ansi FIX_STYLE = Ansi.Red.and(Ansi.Bold);

    @Getter
    private final String message;

    @Getter
    private final String label;

    @Getter
    private final @Nullable String fix;
    private final InsertPosition insertPosition;
    private final ErrorBuilder<? extends CompileError> defaultBuilder;

    /**
     * Constructs a CommonErrors instance with a message, label, optional fix, insertion position,
     * and a default error builder.
     *
     * @param message        A string representing the error message to describe the problem.
     * @param label          A string representing a short label for the error.
     * @param fix            An optional string suggesting a fix for the error, which can be null.
     * @param insertPosition The position relative to the source span where the fix should be inserted.
     * @param defaultBuilder An error builder responsible for constructing instances of {@code CompileError}.
     */
    CommonErrors(String message, String label, @Nullable String fix, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this.message = message;
        this.label = label;
        this.fix = fix;
        this.insertPosition = insertPosition;
        this.defaultBuilder = defaultBuilder;
    }

    /**
     * Constructs a CommonErrors instance with the specified message, label, and default error builder.
     *
     * @param message The error message to be associated with this instance.
     * @param label A label that provides additional context for the error.
     * @param defaultBuilder The default builder used to create instances of the CompileError.
     */
    CommonErrors(String message, String label,ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, (String) null, InsertPosition.AT_SPAN, defaultBuilder);
    }

    /**
     * Constructs a CommonErrors instance with the specified details for handling errors.
     *
     * @param message        the error message providing details about the error
     * @param label          the label associated with the error context
     * @param operator       the operator token associated with the error
     * @param insertPosition the position where the error should be inserted in the content
     * @param defaultBuilder the default error builder used to construct compile errors
     */
    CommonErrors(String message, String label, Tokens.Operator operator, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, operator.toString(), insertPosition, defaultBuilder);
    }

    /**
     * Constructs a CommonErrors instance with a specific error message, label, keyword,
     * insert position, and default error builder.
     *
     * @param message the detailed error message describing the issue
     * @param label the label associated with the error for identification
     * @param keyword the keyword token related to the error context
     * @param insertPosition the position at which the error should be inserted or handled
     * @param defaultBuilder the default error builder to construct instances of CompileError
     */
    CommonErrors(String message, String label, Tokens.Keyword keyword, InsertPosition insertPosition,ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, keyword.toString(), insertPosition, defaultBuilder);
    }

    /**
     * Constructs a CommonErrors instance with the specified message, label, token kind,
     * insert position, and a default error builder.
     *
     * @param message        the error message describing the issue
     * @param label          an optional label providing additional context
     * @param kind           the kind of token related to the error
     * @param insertPosition the position where the error needs to be addressed
     * @param defaultBuilder the default error builder to construct a compile error instance
     */
    CommonErrors(String message, String label, TokenKind kind, InsertPosition insertPosition, ErrorBuilder<? extends CompileError> defaultBuilder) {
        this(message, label, kind.toString(), insertPosition, defaultBuilder);
    }

    /**
     * Constructs and returns a {@link CompileError} using the default error builder
     * with the specified source span.
     *
     * @param span The source span representing the location of the error in the source code.
     * @return A {@link CompileError} instance representing the constructed compile-time error.
     */
    public CompileError toError(SourceSpan span) {
        return defaultBuilder.build(message, span, label, buildHelp(span));
    }

    /**
     * Constructs and returns a {@link CompileError} using the default error builder
     * with the specified source span and an optional label override.
     *
     * @param span          The source span representing the location of the error in the source code.
     * @param labelOverride An optional custom label to override the default error label.
     * @return A {@link CompileError} instance representing the constructed compile-time error.
     */
    public CompileError toError(SourceSpan span, String labelOverride) {
        return defaultBuilder.build(message, span, labelOverride, buildHelp(span));
    }

    /**
     * Constructs and returns an error of the specified type {@code E} by using the provided
     * {@link ErrorBuilder} and parameters.
     *
     * @param <E>     The specific subtype of {@link CompileError} to be constructed.
     * @param span    The source span representing the location of the error in the source code.
     * @param builder The {@link ErrorBuilder} responsible for creating the error instance.
     * @return An instance of type {@code E}, representing the constructed compile-time error.
     */
    public <E extends CompileError> E toError(SourceSpan span, ErrorBuilder<E> builder) {
        return builder.build(message, span, label, buildHelp(span));
    }

    /**
     * Builds an error of the specified type {@code E} using the provided parameters.
     *
     * @param <E>           The specific subtype of {@link CompileError} to be constructed.
     * @param span          The source span representing the location of the error in the source code.
     * @param labelOverride The custom label to override the default error label.
     * @param builder       The {@link ErrorBuilder} responsible for creating the error instance.
     * @return An instance of type {@code E}, representing the constructed compile-time error.
     */
    public <E extends CompileError> E toError(SourceSpan span, String labelOverride, ErrorBuilder<E> builder) {
        return builder.build(message, span, labelOverride, buildHelp(span));
    }

    /**
     * Formats the message and label strings using the provided arguments. It then constructs
     * and returns a {@link CompileError} instance using the default error builder.
     * This method dynamically substitutes the format placeholders in both the message
     * and label strings based on the specified arguments.
     *
     * @param span The source span representing the location of the error in the source code.
     * @param args The arguments to be substituted into the message and label placeholders.
     * @return A {@link CompileError} instance representing the constructed compile-time error.
     */
    public CompileError toFormattedError(SourceSpan span, Object... args) {
        String fmtMessage = String.format(message, args);
        String fmtLabel = String.format(label, args);
        return defaultBuilder.build(fmtMessage, span, fmtLabel, fix != null ? buildHelp(span) : fmtLabel);
    }

    /**
     * Formats the message and the label with the specified arguments, then builds an error
     * instance using the provided {@code labelOverride} and the default error builder.
     * This method dynamically applies the format placeholders in both the message and
     * label strings, creating a compiler error representation.
     *
     * @param span          The source span representing the location of the error in the source code.
     * @param labelOverride The custom label to override the default error label after formatting.
     * @param args          The arguments used to format the message and label placeholders.
     * @return A {@link CompileError} instance representing the constructed compile-time error.
     */
    public CompileError toFormattedErrorWithLabel(SourceSpan span, String labelOverride, Object... args) {
        String fmtMessage = String.format(message, args);
        return defaultBuilder.build(fmtMessage, span, labelOverride, fix != null ? buildHelp(span) : labelOverride);
    }

    /**
     * Formats the message and label with the provided argument values, then builds an error
     * using the specified {@link ErrorBuilder}. This method dynamically applies the format
     * placeholders in the message and label strings based on the passed arguments and constructs
     * an error of the specified type.
     *
     * @param <E>     The specific type of {@link CompileError} to be constructed.
     * @param span    The source span representing the location of the error in the source code.
     * @param builder The {@link ErrorBuilder} responsible for creating the error instance.
     * @param args    The arguments to be substituted into the message and label placeholders.
     * @return An instance of type {@code E}, representing the constructed compile-time error.
     */
    public <E extends CompileError> E toFormattedError(SourceSpan span, ErrorBuilder<E> builder, Object... args) {
        String fmtMessage = String.format(message, args);
        String fmtLabel = String.format(label, args);
        return builder.build(fmtMessage, span, fmtLabel, fix != null ? buildHelp(span) : fmtLabel);
    }

    /**
     * Constructs a help message string based on the given source span and fixes.
     * This method integrates formatting and styling for the fix description into
     * the relevant portion of source code, producing a visually enhanced message
     * for the user. It handles cases of invalid spans, null inputs, or exceptions
     * gracefully, defaulting to the specified label value.
     *
     * @param span The source span representing the location in source code.
     *             If null, invalid, or associated data fails to process, the method
     *             returns the default label.
     * @return A styled help string incorporating contextual source information
     *         with the applied fix, or the default label if the operation fails.
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

    /**
     * Retrieves a specific line of text from the given source string, using a 1-based line index.
     * The method normalizes line endings to ensure consistency across different platforms
     * by converting all line breaks to '\n' before processing.
     *
     * @param source   The input string containing multiple lines of text.
     * @param oneBased The 1-based index of the line to be retrieved.
     *                 A value of 1 corresponds to the first line.
     * @return The specified line of text, or {@code null} if the index is out of bounds.
     */
    private static @Nullable String getLine(String source, int oneBased) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        int idx = oneBased - 1;
        if (idx < 0 || idx >= lines.length) return null;
        return lines[idx];
    }
}
