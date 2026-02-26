package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.error.Error;
import cloth.token.span.SourceSpan;

public class RuntimeError extends Error {

    public RuntimeError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.RUNTIME_ERROR;
    }

}
