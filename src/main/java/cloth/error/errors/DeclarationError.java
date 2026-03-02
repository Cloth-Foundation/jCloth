package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.token.span.SourceSpan;

/**
 * Raised when a declaration violates a structural rule that goes beyond simple
 * token-level syntax — for example, an abstract method that has a body,
 * an interface that declares a field, or a {@code defer} that targets a
 * non-call expression.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class DeclarationError extends CompileError {

    public DeclarationError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.DECLARATION_ERROR;
    }

}
