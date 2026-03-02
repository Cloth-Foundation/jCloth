package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.token.span.SourceSpan;

/**
 * Raised when a declaration uses duplicate, conflicting, or contextually
 * invalid modifiers (e.g. {@code static static}, {@code abstract final},
 * or {@code abstract} on a struct).
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ModifierError extends CompileError {

    public ModifierError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.MODIFIER_ERROR;
    }

}
