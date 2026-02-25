package cloth.error.type;

import cloth.error.ErrorType;

public class CompileError extends Error {

    public CompileError(String message) {
        super(message);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.COMPILE_ERROR;
    }

}
