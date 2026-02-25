package cloth.error.type;

import cloth.error.ErrorType;

public class ParseError extends Error {

    public ParseError(String message) {
        super(message);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.PARSE_ERROR;
    }

}
