package cloth.error;

 import cloth.token.span.SourceLocation;

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
 * - error: Reports an error message with details about the issue.
 * - warning: Reports a warning message with details about the issue.
 * <p>
 * Diagnostics:
 * Both methods generate formatted messages that include the severity level (error or warning),
 * file name, line number, column number, and the detailed message. Messages default to a generic
 * "<unknown>" file name if file information is not available in the provided location.
 */
public class DiagnosticSink {

     public void error(SourceLocation location, String message) {
         System.err.println(format("error", location, message));
     }

     public void warning(SourceLocation location, String message) {
         System.err.println(format("warning", location, message));
     }

     private String format(String level, SourceLocation location, String message) {
         if (location == null) return level + ": " + message;
         String fileName = (location.getFile() == null) ? "<unknown>" : location.getFile().getName();
         return fileName + ":" + location.getLine() + ":" + location.getColumn() + ": " + level + ": " + message;
     }
}
