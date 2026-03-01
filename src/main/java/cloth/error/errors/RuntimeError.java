package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.error.Error;
import cloth.token.span.SourceSpan;

/**
 * Represents a specific type of error that occurs during the runtime phase of a program.
 * This error is used to signal issues encountered while the program is executing, such as
 * invalid operations or unexpected conditions. Instances of this class provide detailed
 * information about the nature, location, and resolution of the runtime issue.
 * <p>
 * This class extends {@link java.lang.Error}, inheriting properties such as the error message,
 * source span, descriptive label, and resolution help. The type of this error is always
 * {@link ErrorType#RUNTIME_ERROR}.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class RuntimeError extends Error {

    public RuntimeError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.RUNTIME_ERROR;
    }

}
