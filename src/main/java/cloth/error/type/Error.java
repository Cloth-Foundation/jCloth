package cloth.error.type;

import cloth.error.ErrorPrinter;
import cloth.error.ErrorType;
import lombok.Getter;

public abstract class Error extends Exception {

    @Getter
    private final String message;

    public Error(String message) {
        this.message = message;
    }

    public abstract ErrorType getType();

    @Override
    public String toString() {
        return message;
    }

    public Error andThrow(int exit) {
        ErrorPrinter.print(this);
        System.exit(exit);
        return this;
    }

}
