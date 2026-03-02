package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.token.span.SourceSpan;

/**
 * Raised when the parser encounters an unexpected token or is missing an
 * expected construct (e.g. a missing semicolon, brace, identifier, or keyword).
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class SyntaxError extends CompileError {

    public SyntaxError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.SYNTAX_ERROR;
    }

}
