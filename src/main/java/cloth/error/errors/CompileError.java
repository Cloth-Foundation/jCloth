package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.error.Error;
import cloth.error.Diagnostic;
import cloth.token.span.SourceSpan;

/**
 * Represents a specific type of error that occurs during the compilation phase of a program.
 * This error is used to signal issues detected at compile time, such as syntax errors or
 * invalid use of language features. Instances of this class provide detailed information
 * about the nature, location, and resolution of the compile-time issue.
 * <p>
 * This class extends {@link java.lang.Error}, inheriting properties like the error message, source span,
 * descriptive label, and resolution help. Additionally, it implements {@link Diagnostic}
 * to provide diagnostic capability for reporting and handling the error.
 * <p>
 * The type of this error is always {@link ErrorType#COMPILE_ERROR}.
 */
public class CompileError extends Error implements Diagnostic {

    public CompileError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.COMPILE_ERROR;
    }

}
