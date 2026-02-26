package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.error.Error;
import cloth.token.span.SourceSpan;

public class ParseError extends Error {

    public ParseError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.PARSE_ERROR;
    }

}
