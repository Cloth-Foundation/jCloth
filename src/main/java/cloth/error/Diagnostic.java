package cloth.error;

import cloth.token.span.SourceSpan;

/**
 * Represents a diagnostic interface used for reporting and handling errors,
 * warnings, or informational messages in a source file. Diagnostics typically
 * provide information about the location of the issue, additional context,
 * and guidance for resolution.
 */
public interface Diagnostic {

    /**
     * Retrieves the span of the diagnostic, which typically represents the range in the
     * source file where the diagnostic or error occurred.
     *
     * @return the {@code SourceSpan} object representing the start and end locations
     *         in the source file, or {@code null} if the span is not applicable or undefined
     */
    SourceSpan getSpan();

    /**
     * Retrieves the label associated with the diagnostic or error,
     * typically used to provide additional context or metadata that
     * supplements the error message.
     *
     * @return a string representing the label, or a default label
     *         if no specific label is provided
     */
    default String getLabel() { return "here"; }

    /**
     * Retrieves a help message that provides additional context or guidance
     * related to the diagnostic or error.
     *
     * @return a string containing the help message, or {@code null} if no
     *         help message is available
     */
    default String getHelp() { return null; }
}
