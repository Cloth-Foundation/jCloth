package cloth.error;

import cloth.token.span.SourceSpan;
import cloth.utility.printers.ErrorPrinter;
import lombok.Getter;

/**
 * Represents an abstract base class for defining specific types of errors.
 * Provides functionality for storing an error message, identifying the type
 * of error, and handling the error by printing and optionally terminating the program.
 * <p>
 * Subclasses must implement the {@link #getType()} method to specify the type of error.
 */
public abstract class Error extends Exception implements Diagnostic {

    /**
     * The error message providing a description of the issue encountered.
     * This message is a human-readable explanation intended to inform users about
     * the nature or cause of the error. It is typically used in error reporting and
     * diagnostics to convey meaningful feedback or context.
     */
    @Getter
    private final String message;

    /**
     * The span within the source code where the error occurred.
     * This variable holds a {@link SourceSpan} object that specifies
     * the precise start and end locations of the error in the source file.
     * It provides context for locating or highlighting the issue, and is
     * used in diagnostics and error handling to indicate where the
     * problem resides.
     */
    @Getter
    private final SourceSpan span;

    /**
     * A descriptive label associated with the error. This label is typically
     * used to provide additional context or metadata that supplements the
     * error message. It helps identify or categorize the error in a concise
     * and informative way.
     */
    @Getter
    private final String label;

    /**
     * Provides guidance or suggestions for addressing the error described by this object.
     * This field is intended to offer actionable information that can help resolve or better
     * understand the error. The content of the help message is specific to the error instance
     * and may vary between different error types or contexts.
     */
    @Getter
    private final String help;

    /**
     * Constructs a new {@code Error} instance with the provided message, source span, label, and help text.
     * The newly created error instance is automatically printed by the {@code ErrorPrinter}.
     *
     * @param message The error message describing the problem.
     * @param span    The source span indicating the location of the error in the source code.
     * @param label   A descriptive label associated with the error.
     * @param help    Guidance or suggestions for addressing the error.
     */
    public Error(String message, SourceSpan span, String label, String help) {
        this.message = message;
        this.span = span;
        this.label = label;
        this.help = help;
        ErrorPrinter.getInstance().print(this);
    }

    /**
     * Retrieves the type of the error represented by this object.
     * Subclasses must implement this method to specify the specific
     * {@code ErrorType} associated with the error.
     *
     * @return the {@code ErrorType} that represents the type of this error
     */
    public abstract ErrorType getType();

    /**
     * Returns the error message associated with this object.
     *
     * @return the error message as a string
     */
    @Override
    public String toString() {
        return message;
    }

    public void exit(int status) {
        System.exit(status);
    }

    public void exit() {
        exit(1);
    }

    public Error getError() {
        return this;
    }

}
