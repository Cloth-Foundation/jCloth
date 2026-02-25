package cloth.error.type;

import cloth.error.ErrorType;

public class RuntimeError extends Error {

    public RuntimeError(String message) {
        super(message);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.RUNTIME_ERROR;
    }

}
