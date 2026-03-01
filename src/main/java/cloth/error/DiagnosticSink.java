package cloth.error;

import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;

/**
 * DiagnosticSink is a utility class used for reporting errors and warnings to the user.
 * It provides methods for printing diagnostic messages with contextual information from
 * the source code, such as file name, line number, and column number, based on the location
 * passed to it.
 * <p>
 * This class is designed for use in environments where feedback needs to be given, such
 * as during lexical or syntactic analysis in a compiler or interpreter.
 * <p>
 * Methods:
 * <ul>
 * <li>error: Reports an error message with details about the issue.</li>
 * <li>warning: Reports a warning message with details about the issue.</li>
 * </ul>
 * <p>
 * Diagnostics:
 * Both methods generate formatted messages that include the severity level (error or warning),
 * file name, line number, column number, and the detailed message. Messages default to a generic
 * "<unknown>" file name if file information is not available in the provided location.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class DiagnosticSink {

    public void error(SourceLocation location, String message) {
        System.err.println(format("error", location, message));
    }

    public void error(SourceSpan span, String message) {
        if (span == null) {
            System.err.println("error: " + message);
            return;
        }
        error(span.start(), message);
    }

    public void warning(SourceLocation location, String message) {
        System.err.println(format("warning", location, message));
    }

    public void warning(SourceSpan span, String message) {
        if (span == null) {
            System.err.println("warning: " + message);
            return;
        }
        warning(span.start(), message);
    }

    private String format(String level, SourceLocation location, String message) {
        if (location == null) return level + ": " + message;
        String fileName = (location.file() == null) ? "<unknown>" : location.file().getName();
        return fileName + ":" + location.line() + ":" + location.column() + ": " + level + ": " + message;
    }
}
